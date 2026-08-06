package com.monsterhouse.booking.repository;

import com.monsterhouse.booking.entity.AvailabilityOverride;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AvailabilityOverrideRepository extends JpaRepository<AvailabilityOverride, Long> {

    Optional<AvailabilityOverride> findByOverrideDate(LocalDate date);

    /** 달력 뷰에서 한 달치를 한 번에 조회 */
    List<AvailabilityOverride> findAllByOverrideDateBetween(LocalDate from, LocalDate to);
}