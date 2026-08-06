package com.monsterhouse.booking.entity;

import com.monsterhouse.common.entity.BaseTimeEntity;
import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.common.exception.BusinessException;
import com.monsterhouse.common.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * ★ 동시성 설계의 핵심 — slot_key
 *
 * 기획서의 UNIQUE(product_id, start_at) 에는 문제가 두 가지 있습니다.
 *  1) 취소된 예약이 남으면 그 슬롯을 영구히 재예약할 수 없음
 *  2) product_id 가 포함되면 "서로 다른 상품끼리의 시간 겹침"을 못 막음
 *
 * 그래서 slot_key 컬럼을 두고
 *  - 취소 시 NULL 로 비웁니다 (InnoDB 유니크 인덱스는 NULL 중복 허용 → 재예약 가능)
 *  - sharedResource=true 면 productId 를 키에서 뺍니다 (촬영팀 1팀 = 시간이 곧 자원)
 *
 * 다만 유니크 제약은 "동일 시작시각"만 막을 뿐 겹침 전체는 못 막으므로,
 * 실제 방어는 BookingService 의 날짜 락 + overlap 쿼리가 담당하고
 * 이 제약은 그것들이 뚫렸을 때의 최후 방어선입니다.
 */
@Getter
@Entity
@Table(
        name = "booking",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_booking_code", columnNames = "booking_code"),
                @UniqueConstraint(name = "uk_booking_slot_key", columnNames = "slot_key")
        },
        indexes = {
                @Index(name = "idx_booking_start_at", columnList = "start_at"),
                @Index(name = "idx_booking_status_start", columnList = "status, start_at"),
                @Index(name = "idx_booking_email", columnList = "email")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Booking extends BaseTimeEntity {

    private static final DateTimeFormatter SLOT_KEY_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 고객이 조회에 쓰는 번호. PK 를 노출하지 않기 위함. */
    @Column(name = "booking_code", nullable = false, length = 30)
    private String bookingCode;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_booking_product"))
    private Product product;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    /** startAt + 소요시간 + 버퍼 */
    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    /** 활성 예약일 때만 값이 있고, 취소되면 null 이 됩니다. */
    @Column(name = "slot_key", length = 40)
    private String slotKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BookingStatus status;

    // ===== 예약자 정보 (비회원, 기획서 §4.2) =====
    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "phone", nullable = false, length = 30)
    private String phone;

    @Column(name = "email", nullable = false, length = 200)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(name = "locale", nullable = false, length = 5)
    private LocaleCode locale;

    @Column(name = "memo", columnDefinition = "TEXT")
    private String memo;

    /** 개인정보 수집·이용 동의 (기획서 §9) */
    @Column(name = "privacy_agreed", nullable = false)
    private boolean privacyAgreed;

    /**
     * 상품가 + 선택 옵션 합계. 예약 시점에 계산해 고정합니다.
     * 상품 가격이 나중에 바뀌어도 이 값은 그대로여야 정산이 맞습니다.
     */
    @Column(name = "total_price", nullable = false, precision = 12, scale = 0)
    private BigDecimal totalPrice;

    /** 상품 기본가 스냅샷 (옵션을 뺀 금액) */
    @Column(name = "base_price", nullable = false, precision = 12, scale = 0)
    private BigDecimal basePrice;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "booking_option",
            joinColumns = @JoinColumn(name = "booking_id",
                    foreignKey = @ForeignKey(name = "fk_booking_option_booking")))
    private List<BookingOption> options = new ArrayList<>();

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "canceled_at")
    private LocalDateTime canceledAt;

    @Column(name = "cancel_reason", length = 300)
    private String cancelReason;

    @Builder
    private Booking(String bookingCode, Product product,
                    LocalDateTime startAt, LocalDateTime endAt, String slotKey,
                    BookingStatus status, String name, String phone, String email,
                    LocaleCode locale, String memo, boolean privacyAgreed,
                    BigDecimal basePrice, List<BookingOption> options) {
        this.bookingCode = bookingCode;
        this.product = product;
        this.startAt = startAt;
        this.endAt = endAt;
        this.slotKey = slotKey;
        this.status = status;
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.locale = locale;
        this.memo = memo;
        this.privacyAgreed = privacyAgreed;

        this.basePrice = basePrice == null ? BigDecimal.ZERO : basePrice;
        this.options = options == null ? new ArrayList<>() : new ArrayList<>(options);
        this.totalPrice = this.options.stream()
                .map(BookingOption::amount)
                .reduce(this.basePrice, BigDecimal::add);

        if (status == BookingStatus.CONFIRMED) {
            this.confirmedAt = LocalDateTime.now();
        }
    }

    /**
     * sharedResource=true  → "202608041400"        (시간 자체가 자원)
     * sharedResource=false → "3@202608041400"      (상품별 독립 자원)
     */
    public static String slotKeyOf(Long productId, LocalDateTime startAt, boolean sharedResource) {
        String time = startAt.format(SLOT_KEY_FORMAT);
        return sharedResource ? time : productId + "@" + time;
    }

    public void confirm() {
        transitionTo(BookingStatus.CONFIRMED);
        this.confirmedAt = LocalDateTime.now();
    }

    public void complete() {
        transitionTo(BookingStatus.COMPLETED);
    }

    /** ★ 취소 시 slot_key 를 비워야 같은 슬롯을 다시 예약할 수 있습니다. */
    public void cancel(String reason) {
        if (this.status == BookingStatus.CANCELED) {
            throw new BusinessException(ErrorCode.ALREADY_CANCELED);
        }
        transitionTo(BookingStatus.CANCELED);
        this.slotKey = null;
        this.canceledAt = LocalDateTime.now();
        this.cancelReason = reason;
    }

    private void transitionTo(BookingStatus next) {
        if (!this.status.canTransitionTo(next)) {
            throw new BusinessException(ErrorCode.INVALID_BOOKING_STATUS);
        }
        this.status = next;
    }

    public boolean isOwnedBy(String email) {
        return this.email != null && this.email.equalsIgnoreCase(email);
    }
}