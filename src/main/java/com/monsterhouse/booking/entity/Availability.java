package com.monsterhouse.booking.entity;

import com.monsterhouse.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;

/** 요일별 기본 영업시간 (기획서 §4.1 운영 일정) */
@Getter
@Entity
@Table(
        name = "availability",
        uniqueConstraints = @UniqueConstraint(name = "uk_availability_dow", columnNames = "day_of_week")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Availability extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 10)
    private DayOfWeek dayOfWeek;

    @Column(name = "open_time", nullable = false)
    private LocalTime openTime;

    @Column(name = "close_time", nullable = false)
    private LocalTime closeTime;

    /** false 면 해당 요일 정기 휴무 */
    @Column(name = "active", nullable = false)
    private boolean active;

    @Builder
    private Availability(DayOfWeek dayOfWeek, LocalTime openTime, LocalTime closeTime, boolean active) {
        this.dayOfWeek = dayOfWeek;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.active = active;
    }

    public void update(LocalTime openTime, LocalTime closeTime, boolean active) {
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.active = active;
    }
}