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

    /** 예약 생성 시 BookingService 가 재검증에 사용합니다. */
    public void validateWithinBusinessHours(Product product, LocalDateTime startAt, LocalDateTime endAt) {
        LocalDate date = startAt.toLocalDate();

        if (isOutOfRange(date)) {
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

        if (startAt.isBefore(earliestBookableAt())) {
            throw new BusinessException(ErrorCode.BOOKING_TOO_LATE, bookingProperties.minLeadHours());
        }
    }

    // ===================== 내부 =====================

    private boolean isOutOfRange(LocalDate date) {
        LocalDate today = LocalDate.now();
        return date.isBefore(today) || date.isAfter(today.plusDays(bookingProperties.maxAdvanceDays()));
    }

    private LocalDateTime earliestBookableAt() {
        return LocalDateTime.now().plusHours(bookingProperties.minLeadHours());
    }

    /**
     * 우선순위: 날짜 예외(SPECIAL/HOLIDAY) > 요일 기본 설정
     * null 이면 그 날은 영업하지 않습니다.
     */
    private BusinessHours resolveBusinessHours(LocalDate date) {
        Optional<AvailabilityOverride> override = overrideRepository.findByOverrideDate(date);

        if (override.isPresent()) {
            AvailabilityOverride ov = override.get();
            if (ov.isHoliday()) {
                return null;
            }
            if (ov.getOpenTime() != null && ov.getCloseTime() != null) {
                return new BusinessHours(ov.getOpenTime(), ov.getCloseTime());
            }
            // SPECIAL 인데 시간이 비어 있으면 요일 기본으로 폴백
        }

        return availabilityRepository
                .findByDayOfWeekAndActiveTrue(date.getDayOfWeek())
                .map(a -> new BusinessHours(a.getOpenTime(), a.getCloseTime()))
                .orElse(null);
    }

    private List<SlotResponse> buildSlots(Product product, LocalDate date, BusinessHours hours) {
        int totalMin = product.getDurationMin() + bookingProperties.bufferMin();
        int step = bookingProperties.slotStepMin();

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

        // 그날의 활성 예약을 한 번만 읽고 메모리에서 겹침을 판정합니다 (N+1 방지)
        List<Booking> occupied = bookingRepository.findOccupying(
                dayStart, dayEnd, BookingStatus.occupyingStatuses());

        LocalDateTime cursor = date.atTime(hours.open());
        LocalDateTime closeAt = date.atTime(hours.close());
        LocalDateTime earliest = earliestBookableAt();

        List<SlotResponse> slots = new ArrayList<>();

        while (true) {
            LocalDateTime slotEnd = cursor.plusMinutes(totalMin);
            if (slotEnd.isAfter(closeAt)) {
                break;
            }

            boolean tooLate = cursor.isBefore(earliest);
            boolean overlapped = overlapsAny(occupied, cursor, slotEnd);

            slots.add(SlotResponse.of(cursor, slotEnd, !tooLate && !overlapped));

            cursor = cursor.plusMinutes(step);
        }

        return slots;
    }

    /** 겹침 판정: start < otherEnd && end > otherStart (등호 제외 — 맞닿는 건 겹침 아님) */
    private boolean overlapsAny(List<Booking> bookings, LocalDateTime start, LocalDateTime end) {
        return bookings.stream().anyMatch(b ->
                start.isBefore(b.getEndAt()) && end.isAfter(b.getStartAt()));
    }

    private boolean isAlignedToGrid(LocalDateTime startAt, LocalTime openTime) {
        long minutesFromOpen = java.time.Duration.between(
                startAt.toLocalDate().atTime(openTime), startAt).toMinutes();
        return minutesFromOpen >= 0 && minutesFromOpen % bookingProperties.slotStepMin() == 0;
    }

    private record BusinessHours(LocalTime open, LocalTime close) {
    }
}