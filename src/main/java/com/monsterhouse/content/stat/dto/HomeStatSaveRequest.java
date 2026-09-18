package com.monsterhouse.content.stat.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * valueNumber(카운트업 숫자)와 valueText(비숫자 값) 중 하나는 있어야 합니다(서비스에서 검증).
 */
public record HomeStatSaveRequest(
        @Min(0)
        Integer valueNumber,
        @Size(max = 10)
        String suffix,
        @Size(max = 40)
        String valueText,
        @Size(max = 60) String labelKo,
        @Size(max = 60) String labelJa,
        @Size(max = 300) String descKo,
        @Size(max = 300) String descJa,
        @Size(max = 300) String photoKey,
        boolean active,
        @Min(0)
        int sortOrder
) {
}
