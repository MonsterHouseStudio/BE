package com.monsterhouse.booking.entity;

import com.monsterhouse.common.enums.LocaleCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 촬영 상품 추가 옵션 (기획서 §4.1 "옵션(헤어메이크업 등)").
 *
 * 실제 운영 예:
 *   - 보정본 1장 추가 : 50,000원 (여러 장 가능 → maxQuantity > 1)
 *   - 포징영상 추가   : 50,000원 (한 번만 → maxQuantity = 1)
 *
 * 옵션을 상품에 매다는 이유:
 *   같은 "보정본 추가"라도 상품마다 값이 달라질 수 있고,
 *   영상 상품에 "보정본 추가"가 뜨면 안 되기 때문입니다.
 */
@Getter
@Entity
@Table(name = "product_option")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_product_option_product"))
    private Product product;

    @Column(name = "name_ko", nullable = false, length = 100)
    private String nameKo;

    @Column(name = "name_ja", length = 100)
    private String nameJa;

    @Column(name = "price", nullable = false, precision = 12, scale = 0)
    private BigDecimal price;

    /** 1 이면 체크박스, 2 이상이면 수량 선택으로 그립니다. */
    @Column(name = "max_quantity", nullable = false)
    private int maxQuantity;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Builder
    private ProductOption(Product product, String nameKo, String nameJa,
                          BigDecimal price, int maxQuantity, int sortOrder) {
        this.product = product;
        this.nameKo = nameKo;
        this.nameJa = nameJa;
        this.price = price;
        this.maxQuantity = Math.max(1, maxQuantity);
        this.sortOrder = sortOrder;
        this.active = true;
    }

    /** 일본어가 없으면 한국어로 폴백합니다. 옵션이 안 보이면 매출이 사라지므로 숨기지 않습니다. */
    public String name(LocaleCode locale) {
        if (locale == LocaleCode.JA && nameJa != null && !nameJa.isBlank()) {
            return nameJa;
        }
        return nameKo;
    }

    public void update(String nameKo, String nameJa, BigDecimal price,
                       int maxQuantity, int sortOrder, boolean active) {
        this.nameKo = nameKo;
        this.nameJa = nameJa;
        this.price = price;
        this.maxQuantity = Math.max(1, maxQuantity);
        this.sortOrder = sortOrder;
        this.active = active;
    }
}
