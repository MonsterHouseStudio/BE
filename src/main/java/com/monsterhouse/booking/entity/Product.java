package com.monsterhouse.booking.entity;

import com.monsterhouse.common.entity.BaseTimeEntity;
import com.monsterhouse.common.enums.LocaleCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.math.BigDecimal;
import java.util.Optional;

/**
 * 촬영 상품.
 * 콘텐츠(post)와 달리 필드가 적고 언어별 독립 발행이 필요 없으므로
 * 번역 테이블 대신 컬럼 분리 방식을 씁니다 (기획서 §8).
 */
@Getter
@Entity
@Table(name = "product")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private ProductType type;
    @Column(name = "name_ko", nullable = false, length = 100)
    private String nameKo;
    @Column(name = "name_ja", length = 100)
    private String nameJa;
    @Column(name = "description_ko", columnDefinition = "TEXT")
    private String descriptionKo;
    @Column(name = "description_ja", columnDefinition = "TEXT")
    private String descriptionJa;
    @Column(name = "duration_min", nullable = false)
    private int durationMin;
    @Column(name = "price", nullable = false, precision = 12, scale = 0)
    private BigDecimal price;
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;
    @Column(name = "active", nullable = false)
    private boolean active;
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    /** 가격 표기 단위. 통역은 /1일, /1시간 이 붙습니다. */
    @Enumerated(EnumType.STRING)
    @Column(name = "price_unit", nullable = false, length = 20)
    private PriceUnit priceUnit;

    /**
     * 온라인 슬롯 예약 대상인가.
     *
     * 통역(대회 서포트·PT)은 대회 일정에 맞춰야 해서 스튜디오 시간표로 잡을 수 없습니다.
     * false 인 상품은 가격표에만 노출되고, 예약은 문의 폼으로 받습니다.
     * BookingService 가 이 값을 검증하므로 API 를 직접 찔러도 예약되지 않습니다.
     */
    @Column(name = "bookable", nullable = false)
    private boolean bookable;

    /** "* 사진촬영 별도" 처럼 가격 옆에 붙는 단서. 설명과 성격이 달라 분리했습니다. */
    @Column(name = "note_ko", length = 300)
    private String noteKo;
    @Column(name = "note_ja", length = 300)
    private String noteJa;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_include", joinColumns = @JoinColumn(name = "product_id", foreignKey = @ForeignKey(name = "fk_product_include_product")))
    private List<ProductInclude> includes = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductOption> options = new ArrayList<>();

    @Builder
    private Product(ProductType type, String nameKo, String nameJa,
                    String descriptionKo, String descriptionJa,
                    int durationMin, BigDecimal price, String currency,
                    boolean active, int sortOrder,
                    PriceUnit priceUnit, boolean bookable,
                    String noteKo, String noteJa) {
        this.type = type;
        this.nameKo = nameKo;
        this.nameJa = nameJa;
        this.descriptionKo = descriptionKo;
        this.descriptionJa = descriptionJa;
        this.durationMin = durationMin;
        this.price = price;
        this.currency = currency == null ? "KRW" : currency;
        this.active = active;
        this.sortOrder = sortOrder;
        this.priceUnit = priceUnit == null ? PriceUnit.PER_SESSION : priceUnit;
        this.bookable = bookable;
        this.noteKo = noteKo;
        this.noteJa = noteJa;
    }

    public String note(LocaleCode locale) {
        if (locale == LocaleCode.JA && noteJa != null && !noteJa.isBlank()) {
            return noteJa;
        }
        return noteKo;
    }

    public void addOption(String nameKo, String nameJa, BigDecimal price,
                          int maxQuantity, int sortOrder) {
        this.options.add(ProductOption.builder()
                .product(this)
                .nameKo(nameKo)
                .nameJa(nameJa)
                .price(price)
                .maxQuantity(maxQuantity)
                .sortOrder(sortOrder)
                .build());
    }

    public List<ProductOption> activeOptions() {
        return options.stream()
                .filter(ProductOption::isActive)
                .sorted(Comparator.comparingInt(ProductOption::getSortOrder))
                .toList();
    }

    /**
     * 일본어가 비어 있으면 한국어로 폴백합니다.
     * 상품은 "미번역이면 숨김"이 아니라 "폴백 노출"이 맞습니다 —
     * 예약을 막으면 안 되니까요. (콘텐츠 폴백 정책과 다름, 기획서 §3.2)
     */
    public String name(LocaleCode locale) {
        if (locale == LocaleCode.JA && nameJa != null && !nameJa.isBlank()) {
            return nameJa;
        }
        return nameKo;
    }
    public String description(LocaleCode locale) {
        if (locale == LocaleCode.JA && descriptionJa != null && !descriptionJa.isBlank()) {
            return descriptionJa;
        }
        return descriptionKo;
    }
    /**
     * 폴백 없이 그 언어에 실제로 저장된 값만 돌려줍니다. 관리자 편집 화면 전용입니다.
     *
     * 관리자 화면에서 includes(JA) 를 쓰면 일본어 미작성 상품의 일본어 칸에
     * 한국어가 채워져 보이고, 그대로 저장하면 한국어가 일본어로 DB 에 박힙니다.
     * 고객 화면은 폴백이 맞지만, 편집 화면은 "빈 것을 빈 것으로" 보여줘야 합니다.
     */
    public List<String> rawIncludes(LocaleCode locale){
        return pickIncludes(locale);
    }

    public List<String> includes(LocaleCode locale){
        List<String> matched = pickIncludes(locale);
        return matched.isEmpty() ? pickIncludes(LocaleCode.DEFAULT) : matched;
    }
    private List<String> pickIncludes(LocaleCode locale){
        return includes.stream()
                .filter(it -> it.getLocale() == locale)
                .sorted(Comparator.comparingInt(ProductInclude::getSortOrder))
                .map(ProductInclude::getContent)
                .toList();
    }
    public void replaceIncludes(LocaleCode locale, List<String> contents){
        this.includes.removeIf(it -> it.getLocale() == locale);
        if(contents == null){
            return;
        }
        for (int i = 0; i < contents.size(); i++){
            String content = contents.get(i);
            if(content != null && !content.isBlank()){
                this.includes.add(new ProductInclude(locale, content.trim(), i));
            }
        }
    }
    public void update(String nameKo, String nameJa, String descriptionKo, String descriptionJa,
                       int durationMin, BigDecimal price, int sortOrder,
                       PriceUnit priceUnit, boolean bookable, String noteKo, String noteJa) {
        this.nameKo = nameKo;
        this.nameJa = nameJa;
        this.descriptionKo = descriptionKo;
        this.descriptionJa = descriptionJa;
        this.durationMin = durationMin;
        this.price = price;
        this.sortOrder = sortOrder;
        this.priceUnit = priceUnit == null ? PriceUnit.PER_SESSION : priceUnit;
        this.bookable = bookable;
        this.noteKo = noteKo;
        this.noteJa = noteJa;
    }
    public void activate() {
        this.active = true;
    }
    public void deactivate() {
        this.active = false;
    }
    /**
     * 옵션을 id 로 찾습니다.
     * 다른 상품의 옵션 id 가 넘어오는 걸 막는 역할도 합니다
     * (5만원 상품에 남의 상품 저가 옵션을 붙이는 식의 조작 차단).
     */
    public java.util.Optional<ProductOption> findOption(Long optionId) {
        return options.stream()
                .filter(o -> o.getId() != null && o.getId().equals(optionId))
                .findFirst();
    }

    /** active 까지 지정하는 관리자용 오버로드 */
    public ProductOption addOption(String nameKo, String nameJa, BigDecimal price,
                                   int maxQuantity, int sortOrder, boolean active) {
        ProductOption option = ProductOption.builder()
                .product(this)
                .nameKo(nameKo)
                .nameJa(nameJa)
                .price(price)
                .maxQuantity(maxQuantity)
                .sortOrder(sortOrder)
                .build();

        // 빌더는 active 를 항상 true 로 만듭니다(생성자 참고).
        // 비활성으로 만들어야 하면 곧바로 update 로 내립니다.
        if (!active) {
            option.update(nameKo, nameJa, price, maxQuantity, sortOrder, false);
        }

        this.options.add(option);
        return option;
    }

    public void removeOption(ProductOption option) {
        // orphanRemoval = true 라 컬렉션에서 빼면 DB 에서도 삭제됩니다.
        this.options.remove(option);
    }
}