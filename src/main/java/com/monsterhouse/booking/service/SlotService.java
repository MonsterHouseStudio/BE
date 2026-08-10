package com.monsterhouse.booking.service;

import com.monsterhouse.booking.dto.response.DailySlotsResponse;
import com.monsterhouse.booking.dto.response.SlotResponse;
import com.monsterhouse.booking.entity.*;
import com.monsterhouse.booking.repository.AvailabilityOverrideRepository;
import com.monsterhouse.booking.repository.AvailabilityRepository;
import com.monsterhouse.booking.repository.BookingRepository;
import com.monsterhouse.booking.repository.ProductRepository;
import com.monsterhouse.common.config.BookingProperties;
import com.monsterhouse.common.exception.BusinessException;
import com.monsterhouse.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 슬롯은 DB 에 미리 만들지 않고 조회 시점에 계산합니다.
 *
 * 이유:
 *  - 상품마다 소요시간이 달라 "상품 × 날짜 × 시각" 조합을 미리 INSERT 하면 폭증합니다.
 *  - 영업시간/휴무를 바꿀 때마다 미래 슬롯 전체를 재생성해야 해서 운영이 취약해집니다.
 *
 * 대신 "락 걸 행이 없다"는 문제가 생기는데, 그건 BookingDayLock 이 해결합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SlotService {

    private final ProductRepository productRepository;
    private final AvailabilityRepository availabilityRepository;
    private final AvailabilityOverrideRepository overrideRepository;
    private final BookingRepository bookingRepository;
    private final BookingProperties bookingProperties;

    public DailySlotsResponse getDailySlots(Long productId, LocalDate date) {
        Product product = productRepository.findByIdAndActiveTrue(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        if (isOutOfRange(date)) {
            return DailySlotsResponse.closed(productId, date, "OUT_OF_RANGE");
        }

        BusinessHours hours = resolveBusinessHours(date);
        if (hours == null) {
            return DailySlotsResponse.closed(productId, date, "CLOSED_DAY");
        }

        List<SlotResponse> slots = buildSlots(product, date, hours);
        return DailySlotsResponse.open(productId, date, slots);
    }

    /** 달력 뷰용 — 날짜별로 "예약 가능한 슬롯이 하나라도 있는지"만 판정 */
    public List<LocalDate> getBookableDates(Long productId, LocalDate from, LocalDate to) {
        List<LocalDate> result = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            DailySlotsResponse daily = getDailySlots(productId, d);
            boolean hasAvailable = daily.open()
                    && daily.slots().stream().anyMatch(SlotResponse::available);
            if (hasAvailable) {
                result.add(d);
            }
        }
        return result;
    }

    /**
     * 예약 생성 시 BookingService 가 재검증에 사용합니다.
     * 고객 요청용 — 리드타임·예약 가능 기간까지 전부 검사합니다.
     */
    public void validateWithinBusinessHours(Product product, LocalDateTime startAt, LocalDateTime endAt) {
        validateWithinBusinessHours(product, startAt, endAt, true);
    }

    /**
     * enforceCustomerPolicy = false 는 관리자 전용입니다.
     *
     * 영업시간·휴무일·슬롯 정렬은 그대로 지키되, 고객 대상 정책만 건너뜁니다:
     *   · "촬영 24시간 전까지" (minLeadHours)
     *   · "90일 이내만" (maxAdvanceDays)
     *
     * 사장님이 전화로 합의한 내일 촬영을 시스템이 막으면 안 됩니다.
     * 반대로 영업시간 밖이나 휴무일에 꽂히는 건 관리자라도 막아야 합니다 —
     * 그건 합의가 아니라 실수일 가능성이 높습니다.
     */
    public void validateWithinBusinessHours(Product product, LocalDateTime startAt,
                                            LocalDateTime endAt, boolean enforceCustomerPolicy) {
        LocalDate date = startAt.toLocalDate();

        if (enforceCustomerPolicy && isOutOfRange(date)) {
            throw new BusinessException(ErrorCode.OUT_OF_BOOKING_RANGE);
        }

        BusinessHours hours = resolveBusinessHours(date);
        if (hours == null) {
            throw new BusinessException(ErrorCode.CLOSED_DAY);
        }

        LocalDateTime open = date.atTime(hours.open());
        LocalDateTime close = date.atTime(hours.close());

        // 마감 시각 이전에 촬영이 끝나야 합니다.
        if (startAt.isBefore(open) || endAt.isAfter(close)) {
            throw new BusinessException(ErrorCode.SLOT_UNAVAILABLE);
        }

        // 슬롯 그리드에 정렬되지 않은 임의 시각은 거부 (클라이언트 조작 방지)
        if (!isAlignedToGrid(startAt, hours.open())) {
            throw new BusinessException(ErrorCode.SLOT_UNAVAILABLE);
        }

        if (enforceCustomerPolicy && startAt.isBefore(earliestBookableAt())) {
            throw new BusinessException(ErrorCode.BOOKING_TOO_LATE, bookingProperties.minLeadHours());
        }
    }

    /**
     * ★ 이미 잡혀 있는 예약을 고객이 지금 건드릴 수 있는 시점인지.
     *
     * 이 검사가 없으면 촬영 1시간 전에 다음 달로 미루는 게 가능해집니다.
     * 사실상 무단 취소인데 취소 정책은 피해가는 구멍이 됩니다.
     * (새 시간이 리드타임을 지키는지는 validateWithinBusinessHours 가 따로 봅니다)
     */
    public void validateChangeableNow(LocalDateTime currentStartAt) {
        if (currentStartAt.isBefore(earliestBookableAt())) {
            throw new BusinessException(ErrorCode.BOOKING_TOO_LATE, bookingProperties.minLeadHours());
        }
    }

    // ===================== 내부 =====================

    /** 오늘 이전이거나 예약 가능 기간(maxAdvanceDays)을 넘어선 날짜인가. */
    private boolean isOutOfRange(LocalDate date) {
        LocalDate today = LocalDate.now();
        return date.isBefore(today)
                || date.isAfter(today.plusDays(bookingProperties.maxAdvanceDays()));
    }

    /** 지금부터 최소 리드타임이 지난 시각. 이보다 이른 슬롯은 예약할 수 없습니다. */
    private LocalDateTime earliestBookableAt() {
        return LocalDateTime.now().plusHours(bookingProperties.minLeadHours());
    }

    /**
     * 그 날의 영업시간. null 이면 휴무입니다.
     *
     * 특정 날짜 예외(availability_exception)가 요일 기본값보다 우선합니다.
     * 예외가 휴무면 즉시 null, 시간만 다르면 그 시간을 씁니다.
     * 예외 행은 있는데 시간이 비어 있으면 요일 기본값으로 떨어집니다.
     */
    private BusinessHours resolveBusinessHours(LocalDate date) {
        Optional<AvailabilityOverride> override = overrideRepository.findByOverrideDate(date);

        if (override.isPresent()) {
            AvailabilityOverride exception = override.get();

            if (exception.isHoliday()) {
                return null;
            }
            if (exception.getOpenTime() != null && exception.getCloseTime() != null) {
                return new BusinessHours(exception.getOpenTime(), exception.getCloseTime());
            }
        }

        return availabilityRepository.findByDayOfWeekAndActiveTrue(date.getDayOfWeek())
                .map(availability -> new BusinessHours(
                        availability.getOpenTime(), availability.getCloseTime()))
                .orElse(null);
    }

    /**
     * 슬롯 목록을 계산합니다. DB 에 미리 만들어두지 않고 매번 계산합니다(클래스 주석 참고).
     *
     * available=false 인 슬롯도 포함해서 내려줍니다.
     * 마감된 시간을 화면에서 지워버리면 사용자가 "왜 이 시간이 없지"를 알 수 없습니다.
     */
    private List<SlotResponse> buildSlots(Product product, LocalDate date, BusinessHours hours) {
        int blockMin = product.getDurationMin() + bookingProperties.bufferMin();
        int stepMin = bookingProperties.slotStepMin();

        // 전날 시작해 오늘로 넘어오는 예약까지 잡아야 하므로 하루 전체를 겹침 기준으로 조회합니다.
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

        List<Booking> occupying = bookingRepository.findOccupying(
                dayStart, dayEnd, BookingStatus.occupyingStatuses());

        LocalDateTime cursor = date.atTime(hours.open());
        LocalDateTime close = date.atTime(hours.close());
        LocalDateTime earliest = earliestBookableAt();

        List<SlotResponse> slots = new ArrayList<>();

        while (true) {
            LocalDateTime slotEnd = cursor.plusMinutes(blockMin);

            // 마감 시각 안에 촬영이 끝나지 않으면 더 이상 슬롯이 없습니다.
            if (slotEnd.isAfter(close)) {
                break;
            }

            boolean tooLate = cursor.isBefore(earliest);
            boolean taken = overlapsAny(occupying, cursor, slotEnd);

            slots.add(SlotResponse.of(cursor, slotEnd, !tooLate && !taken));

            cursor = cursor.plusMinutes(stepMin);
        }

        return slots;
    }

    /**
     * 겹침 조건: A.start < B.end AND A.end > B.start
     * 등호를 넣지 않는 것이 핵심입니다 — 10:00~11:30 과 11:30~13:00 은 겹치지 않습니다.
     * (BookingRepository.existsOverlap 과 같은 규칙이어야 합니다)
     */
    private boolean overlapsAny(List<Booking> bookings, LocalDateTime start, LocalDateTime end) {
        return bookings.stream()
                .anyMatch(b -> start.isBefore(b.getEndAt()) && end.isAfter(b.getStartAt()));
    }

    /**
     * 개점 시각부터 slotStepMin 간격의 격자에 정확히 올라가 있는가.
     * 클라이언트가 10:07 같은 임의 시각을 보내는 조작을 막습니다.
     */
    private boolean isAlignedToGrid(LocalDateTime startAt, LocalTime openTime) {
        long minutes = Duration.between(startAt.toLocalDate().atTime(openTime), startAt).toMinutes();
        return minutes >= 0 && minutes % bookingProperties.slotStepMin() == 0;
    }

    private record BusinessHours(LocalTime open, LocalTime close) {
    }
}