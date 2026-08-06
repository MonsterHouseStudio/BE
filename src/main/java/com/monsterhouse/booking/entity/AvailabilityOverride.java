package com.monsterhouse.booking.entity;

import com.monsterhouse.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 특정 날짜의 예외 운영 (기획서의 availability_exception).
 * 클래스명을 Override 로 둔 이유: 코드베이스에 BusinessException 등이 있어
 * "...Exception" 이면 예외 클래스로 오해됩니다. 테이블명은 기획서대로 유지.
 */
@Getter
@Entity
@Table(
        name = "availability_exception",
        uniqueConstraints = @UniqueConstraint(name = "uk_availability_exception_date",
                columnNames = "override_date")
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AvailabilityOverride extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "override_date", nullable = false)
    private LocalDate overrideDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private OverrideType type;

    /** SPECIAL 일 때만 사용 */
    @Column(name = "open_time")
    private LocalTime openTime;

    @Column(name = "close_time")
    private LocalTime closeTime;

    @Column(name = "memo", length = 200)
    private String memo;

    @Builder
    private AvailabilityOverride(LocalDate overrideDate, OverrideType type,
                                 LocalTime openTime, LocalTime closeTime, String memo) {
        this.overrideDate = overrideDate;
        this.type = type;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.memo = memo;
    }

    public boolean isHoliday() {
        return type == OverrideType.HOLIDAY;
    }

    public void update(OverrideType type, LocalTime openTime, LocalTime closeTime, String memo) {
        this.type = type;
        this.openTime = openTime;
        this.closeTime = closeTime;
        this.memo = memo;
    }
}