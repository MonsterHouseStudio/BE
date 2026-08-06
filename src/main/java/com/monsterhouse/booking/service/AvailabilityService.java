package com.monsterhouse.booking.service;

import com.monsterhouse.booking.dto.request.AvailabilityOverrideRequest;
import com.monsterhouse.booking.dto.request.AvailabilityUpsertRequest;
import com.monsterhouse.booking.dto.response.AvailabilityOverrideResponse;
import com.monsterhouse.booking.dto.response.AvailabilityResponse;
import com.monsterhouse.booking.entity.Availability;
import com.monsterhouse.booking.entity.AvailabilityOverride;
import com.monsterhouse.booking.entity.OverrideType;
import com.monsterhouse.booking.repository.AvailabilityOverrideRepository;
import com.monsterhouse.booking.repository.AvailabilityRepository;
import com.monsterhouse.common.exception.BusinessException;
import com.monsterhouse.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AvailabilityService {

    private final AvailabilityRepository availabilityRepository;
    private final AvailabilityOverrideRepository overrideRepository;

    /**
     * 항상 7개를 돌려줍니다.
     * DB 에 행이 없는 요일도 "미설정(비활성)" 상태로 채워야
     * 관리자 화면에서 요일이 빠져 보이지 않습니다.
     */
    public List<AvailabilityResponse> findWeek() {
        Map<DayOfWeek, Availability> saved = availabilityRepository.findAll().stream()
                .collect(Collectors.toMap(Availability::getDayOfWeek, Function.identity()));

        return Arrays.stream(DayOfWeek.values())
                .map(day -> saved.containsKey(day)
                        ? AvailabilityResponse.of(saved.get(day))
                        : AvailabilityResponse.empty(day))
                .toList();
    }

    @Transactional
    public AvailabilityResponse upsert(AvailabilityUpsertRequest request) {
        validateTimeRange(request.openTime(), request.closeTime());

        Availability availability = availabilityRepository
                .findByDayOfWeek(request.dayOfWeek())
                .orElseGet(() -> availabilityRepository.save(Availability.builder()
                        .dayOfWeek(request.dayOfWeek())
                        .openTime(request.openTime())
                        .closeTime(request.closeTime())
                        .active(request.active())
                        .build()));

        availability.update(request.openTime(), request.closeTime(), request.active());

        log.info("Availability updated. day={} {}~{} active={}",
                request.dayOfWeek(), request.openTime(), request.closeTime(), request.active());

        return AvailabilityResponse.of(availability);
    }

    // ===== 날짜 예외 (임시 휴무 / 특별 영업) =====

    public List<AvailabilityOverrideResponse> findOverrides(LocalDate from, LocalDate to) {
        return overrideRepository.findAllByOverrideDateBetween(from, to).stream()
                .map(AvailabilityOverrideResponse::of)
                .toList();
    }

    @Transactional
    public AvailabilityOverrideResponse saveOverride(AvailabilityOverrideRequest request) {
        if (request.type() == OverrideType.SPECIAL) {
            if (request.openTime() == null || request.closeTime() == null) {
                // SPECIAL 인데 시간이 없으면 SlotService 가 요일 기본값으로 폴백해버려
                // "특별 영업을 등록했는데 아무것도 안 바뀌는" 상태가 됩니다.
                throw new BusinessException(ErrorCode.INVALID_INPUT);
            }
            validateTimeRange(request.openTime(), request.closeTime());
        }

        AvailabilityOverride override = overrideRepository
                .findByOverrideDate(request.date())
                .orElseGet(() -> overrideRepository.save(AvailabilityOverride.builder()
                        .overrideDate(request.date())
                        .type(request.type())
                        .openTime(request.openTime())
                        .closeTime(request.closeTime())
                        .memo(request.memo())
                        .build()));

        override.update(request.type(), request.openTime(), request.closeTime(), request.memo());

        log.info("Availability override saved. date={} type={}", request.date(), request.type());

        return AvailabilityOverrideResponse.of(override);
    }

    @Transactional
    public void deleteOverride(Long overrideId) {
        AvailabilityOverride override = overrideRepository.findById(overrideId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
        overrideRepository.delete(override);
    }

    private void validateTimeRange(java.time.LocalTime open, java.time.LocalTime close) {
        if (!open.isBefore(close)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }
}