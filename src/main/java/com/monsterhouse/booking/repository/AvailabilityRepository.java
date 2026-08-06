package com.monsterhouse.booking.repository;

import com.monsterhouse.booking.entity.Availability;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public interface AvailabilityRepository extends JpaRepository<Availability, Long> {

    Optional<Availability> findByDayOfWeek(DayOfWeek dayOfWeek);

    Optional<Availability> findByDayOfWeekAndActiveTrue(DayOfWeek dayOfWeek);

    List<Availability> findAllByOrderByDayOfWeekAsc();
}