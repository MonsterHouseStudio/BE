package com.monsterhouse.booking.entity;

import com.monsterhouse.common.enums.LocaleCode;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductInclude {
    @Enumerated(EnumType.STRING)
    @Column(name = "locale", nullable = false, length = 5)
    private LocaleCode locale;
    @Column(name = "content", nullable = false, length = 200)
    private String content;
    @Column(name = "sort_order", nullable = false)
    private int sortOrder;
    public ProductInclude(LocaleCode locale, String content, int sortOrder){
        this.locale = locale;
        this.content = content;
        this.sortOrder = sortOrder;
    }
}
