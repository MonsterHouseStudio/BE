package com.monsterhouse.booking.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 예약에 선택된 옵션 한 줄.
 *
 * ★ 이름과 단가를 "그때의 값으로 복사해" 저장합니다.
 *
 * product_option 을 FK 로만 참조하면, 나중에 옵션 가격을 5만 원 → 7만 원으로 올렸을 때
 * 과거 예약의 결제 금액이 소급해서 바뀝니다. 정산과 분쟁의 근거가 무너집니다.
 * 옵션이 삭제되면 아예 무슨 옵션이었는지도 사라집니다.
 * 그래서 스냅샷을 남깁니다.
 */
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookingOption {

    /** 참조용. 이 값이 없어도 이름·단가로 내역을 복원할 수 있습니다. */
    @Column(name = "option_id")
    private Long optionId;

    @Column(name = "option_name", nullable = false, length = 100)
    private String optionName;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 0)
    private BigDecimal unitPrice;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    public BookingOption(Long optionId, String optionName, BigDecimal unitPrice, int quantity) {
        this.optionId = optionId;
        this.optionName = optionName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }

    public BigDecimal amount() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
