package com.monsterhouse.booking.service;

import com.monsterhouse.booking.dto.request.BookingCancelRequest;
import com.monsterhouse.booking.dto.request.BookingCreateRequest;
import com.monsterhouse.booking.dto.response.BookingResponse;
import com.monsterhouse.booking.entity.Booking;
import com.monsterhouse.booking.entity.BookingOption;
import com.monsterhouse.booking.entity.ProductOption;
import com.monsterhouse.booking.entity.BookingStatus;
import com.monsterhouse.booking.entity.Product;
import com.monsterhouse.booking.exception.SlotAlreadyTakenException;
import com.monsterhouse.booking.repository.BookingDayLockRepository;
import com.monsterhouse.booking.repository.BookingRepository;
import com.monsterhouse.booking.repository.ProductRepository;
import com.monsterhouse.common.config.BookingProperties;
import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.common.exception.BusinessException;
import com.monsterhouse.common.exception.ErrorCode;
import com.monsterhouse.notification.event.BookingStatusChangedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * ★★★ 이 프로젝트의 기술적 심장부 (기획서 §4.3)
 *
 * 동시 요청 3중 방어:
 *
 *   1층  booking_day_lock 행에 SELECT ... FOR UPDATE
 *        → 같은 날짜에 대한 예약 요청을 완전히 직렬화합니다.
 *          이게 없으면 2층 검사와 INSERT 사이에 다른 트랜잭션이 끼어듭니다.
 *
 *   2층  overlap 쿼리
 *        → 시간대 겹침 전체를 잡습니다. 유니크 제약으로는 표현 불가능한 조건입니다.
 *
 *   3층  UNIQUE(slot_key) 제약
 *        → 애플리케이션 로직에 버그가 생겨도 DB 가 동일 시작시각 중복을 막습니다.
 *          최후 방어선이므로 절대 제거하지 말 것.
 *
 * 알림은 반드시 트랜잭션 밖(커밋 후)에서 나갑니다.
 * 롤백된 예약의 확정 메일이 나가면 되돌릴 수 없습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookingService {

    private final BookingRepository bookingRepository;
    private final BookingDayLockRepository dayLockRepository;
    private final BookingDayLockManager dayLockManager;
    private final ProductRepository productRepository;
    private final SlotService slotService;
    private final BookingCodeGenerator codeGenerator;
    private final BookingProperties bookingProperties;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * ★ READ_COMMITTED 가 반드시 필요합니다.
     *
     * MySQL 기본값은 REPEATABLE READ 이고, 이때 일반 SELECT 는 트랜잭션의 첫 읽기 시점에
     * 만들어진 스냅샷을 계속 봅니다. 그래서 이런 일이 벌어집니다.
     *
     *   T2: findById(product)          ← 여기서 스냅샷 확정
     *   T1: 예약 INSERT 후 커밋
     *   T2: 날짜 락 획득 (FOR UPDATE 는 최신 데이터를 보지만)
     *   T2: existsOverlap(...)         ← 일반 읽기라 낡은 스냅샷! T1 의 예약이 안 보임
     *   T2: INSERT 성공 → 겹치는 예약 2건 생성
     *
     * 락으로 순서를 세워도 "보는 데이터가 과거"면 검사 자체가 무의미합니다.
     * 실제로 이 설정이 없을 때 "상품이 달라도 겹치면 1건만" 테스트가 2건으로 깨졌습니다.
     * (같은 시각이면 UNIQUE(slot_key) 가 막아주지만, 상품이 다르면 slot_key 가 달라 그것도 못 막습니다)
     *
     * READ_COMMITTED 로 두면 문장마다 최신 커밋 데이터를 읽습니다.
     * 갭 락도 줄어들어 데드락 위험이 함께 낮아집니다.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BookingResponse create(BookingCreateRequest request, LocaleCode locale) {
        // honeypot — 봇에게는 성공한 것처럼 보이게 하지 않고 조용히 거부
        if (request.isBot()) {
            log.info("Honeypot triggered. email={}", request.email());
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (!product.isActive()) {
            throw new BusinessException(ErrorCode.PRODUCT_INACTIVE);
        }

        // 통역처럼 슬롯 예약 대상이 아닌 상품은 화면에 예약 버튼이 없지만,
        // API 를 직접 호출하면 뚫립니다. 서버에서 막습니다.
        if (!product.isBookable()) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_BOOKABLE);
        }

        LocalDateTime startAt = request.startAt().withSecond(0).withNano(0);
        LocalDateTime endAt = startAt.plusMinutes(
                product.getDurationMin() + bookingProperties.bufferMin());

        // 영업시간·휴무·리드타임·그리드 정렬 검증 (클라이언트를 신뢰하지 않습니다)
        slotService.validateWithinBusinessHours(product, startAt, endAt);

        // ───── 1층: 날짜 락 획득. 이 시점부터 같은 날짜 요청은 줄을 섭니다 ─────
        acquireDayLock(startAt.toLocalDate());

        // ───── 2층: 겹침 검사 ─────
        if (isOverlapping(product.getId(), startAt, endAt)) {
            throw new SlotAlreadyTakenException();
        }

        BookingStatus initialStatus = bookingProperties.autoConfirm()
                ? BookingStatus.CONFIRMED
                : BookingStatus.REQUESTED;

        Booking booking = Booking.builder()
                .bookingCode(codeGenerator.generate(startAt.toLocalDate()))
                .product(product)
                .startAt(startAt)
                .endAt(endAt)
                .slotKey(Booking.slotKeyOf(
                        product.getId(), startAt, bookingProperties.sharedResource()))
                .status(initialStatus)
                .name(request.name())
                .phone(request.phone())
                .email(request.email())
                .locale(locale)
                .memo(request.memo())
                .privacyAgreed(request.privacyAgreed())
                .basePrice(product.getPrice())
                .options(resolveOptions(product, request.options()))
                .build();

        // ───── 3층: UNIQUE(slot_key). flush 로 지금 위반을 확인합니다 ─────
        try {
            bookingRepository.saveAndFlush(booking);
        } catch (DataIntegrityViolationException e) {
            // 1·2층을 뚫고 여기까지 왔다면 로직에 구멍이 있다는 신호입니다. WARN 으로 남깁니다.
            log.warn("Unique constraint caught a duplicated slot. slotKey={}", booking.getSlotKey(), e);
            throw new SlotAlreadyTakenException();
        }

        // 커밋 후에만 알림이 나가도록 이벤트만 발행합니다 (§4.3 트랜잭션 경계)
        eventPublisher.publishEvent(
                new BookingStatusChangedEvent(booking.getId(), null, booking.getStatus()));

        log.info("Booking created. code={} product={} startAt={}",
                booking.getBookingCode(), product.getId(), startAt);

        return BookingResponse.of(booking, locale);
    }

    /** 고객 조회 — 예약번호 + 이메일 본인 확인 */
    public BookingResponse findByCode(String bookingCode, String email, LocaleCode locale) {
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND));

        if (!booking.isOwnedBy(email)) {
            // 존재 여부를 흘리지 않기 위해 NOT_FOUND 로 통일합니다.
            throw new BusinessException(ErrorCode.BOOKING_NOT_FOUND);
        }

        return BookingResponse.of(booking, locale);
    }

    /** 고객 취소 */
    @Transactional
    public BookingResponse cancelByCustomer(String bookingCode, BookingCancelRequest request,
                                            LocaleCode locale) {
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND));

        if (!booking.isOwnedBy(request.email())) {
            throw new BusinessException(ErrorCode.BOOKING_NOT_FOUND);
        }

        BookingStatus before = booking.getStatus();
        booking.cancel(request.reason());

        eventPublisher.publishEvent(
                new BookingStatusChangedEvent(booking.getId(), before, BookingStatus.CANCELED));

        return BookingResponse.of(booking, locale);
    }

    // ===================== 관리자 =====================

    @Transactional
    public BookingResponse confirm(Long bookingId) {
        Booking booking = getOrThrow(bookingId);
        BookingStatus before = booking.getStatus();
        booking.confirm();

        eventPublisher.publishEvent(
                new BookingStatusChangedEvent(booking.getId(), before, BookingStatus.CONFIRMED));

        return BookingResponse.of(booking, booking.getLocale());
    }

    @Transactional
    public BookingResponse complete(Long bookingId) {
        Booking booking = getOrThrow(bookingId);
        booking.complete();
        return BookingResponse.of(booking, booking.getLocale());
    }

    @Transactional
    public BookingResponse cancelByAdmin(Long bookingId, String reason) {
        Booking booking = getOrThrow(bookingId);
        BookingStatus before = booking.getStatus();
        booking.cancel(reason);

        eventPublisher.publishEvent(
                new BookingStatusChangedEvent(booking.getId(), before, BookingStatus.CANCELED));

        return BookingResponse.of(booking, booking.getLocale());
    }

    // ===================== 내부 =====================

    /**
     * 날짜 락을 잡습니다. 이 시점부터 같은 날짜의 예약 요청은 한 줄로 서게 됩니다.
     *
     * ★ 순서가 중요합니다. 반드시 "행 보장 → FOR UPDATE" 여야 합니다.
     *
     * 반대로 하면(없는 행에 FOR UPDATE 먼저) InnoDB 가 갭 락을 잡고,
     * 동시 요청들이 서로의 갭 락 때문에 INSERT 하지 못해 데드락에 빠집니다.
     * 자세한 이유는 BookingDayLockManager 주석 참고.
     *
     * ensureExists 는 이미 행이 있으면 아무 일도 하지 않으므로(INSERT IGNORE)
     * 매번 호출해도 비용이 거의 없습니다.
     */
    private void acquireDayLock(LocalDate date) {
        dayLockManager.ensureExists(date);

        dayLockRepository.findForUpdate(date)
                .orElseThrow(() -> {
                    log.error("Failed to acquire booking day lock. date={}", date);
                    return new BusinessException(ErrorCode.INTERNAL_ERROR);
                });
    }

    private boolean isOverlapping(Long productId, LocalDateTime startAt, LocalDateTime endAt) {
        if (bookingProperties.sharedResource()) {
            // 촬영팀이 1팀 — 상품과 무관하게 시간이 겹치면 불가
            return bookingRepository.existsOverlap(
                    startAt, endAt, BookingStatus.occupyingStatuses());
        }
        return bookingRepository.existsOverlapByProduct(
                productId, startAt, endAt, BookingStatus.occupyingStatuses());
    }

    /**
     * 선택된 옵션을 검증하고 스냅샷으로 바꿉니다.
     *
     * 클라이언트가 보낸 건 optionId 와 수량뿐입니다. 가격은 절대 받지 않습니다.
     * 금액을 요청 본문에서 받으면 그 값을 0 으로 바꿔 보내는 순간 공짜 예약이 됩니다.
     *
     * 다른 상품의 옵션 id 를 끼워 넣는 것도 막습니다.
     * (5만원짜리 상품에 다른 상품의 저렴한 옵션을 붙이는 식의 조작)
     */
    private List<BookingOption> resolveOptions(
            Product product, List<BookingCreateRequest.OptionSelection> selections) {

        if (selections == null || selections.isEmpty()) {
            return List.of();
        }

        Map<Long, ProductOption> available = product.activeOptions().stream()
                .collect(Collectors.toMap(ProductOption::getId, Function.identity()));

        List<BookingOption> resolved = new ArrayList<>();

        for (BookingCreateRequest.OptionSelection selection : selections) {
            ProductOption option = available.get(selection.optionId());

            if (option == null) {
                log.info("Unknown option for product. productId={} optionId={}",
                        product.getId(), selection.optionId());
                throw new BusinessException(ErrorCode.INVALID_OPTION);
            }
            if (selection.quantity() < 1 || selection.quantity() > option.getMaxQuantity()) {
                throw new BusinessException(ErrorCode.INVALID_OPTION);
            }

            resolved.add(new BookingOption(
                    option.getId(),
                    option.name(LocaleCode.KO),   // 운영자가 읽는 값이라 한국어로 고정
                    option.getPrice(),
                    selection.quantity()));
        }

        return resolved;
    }

    private Booking getOrThrow(Long bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOKING_NOT_FOUND));
    }
}