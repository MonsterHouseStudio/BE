package com.monsterhouse.content.about.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CrewSaveRequest(
        @NotBlank @Size(max = 60) String nameKo,
        @Size(max = 60) String nameJa,
        @Size(max = 60) String roleKo,
        @Size(max = 60) String roleJa,
        @Size(max = 500) String bioKo,
        @Size(max = 500) String bioJa,
        @Size(max = 300) String photoKey,
        boolean active,
        @Min(0) int sortOrder
) {
}
