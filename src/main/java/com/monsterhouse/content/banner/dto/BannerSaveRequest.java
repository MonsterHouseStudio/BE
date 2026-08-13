package com.monsterhouse.content.banner.dto;

import com.monsterhouse.content.banner.entity.BannerMediaType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record BannerSaveRequest(
        @NotNull
        BannerMediaType mediaType,
        @NotBlank
        @Size(max = 300)
        String mediaKey,
        @Size(max = 300)
        String posterKey,
        @Size(max = 200) String headlineKo,
        @Size(max = 200) String headlineJa,
        @Size(max = 500) String subtextKo,
        @Size(max = 500) String subtextJa,
        boolean active,
        @Min(0)
        int sortOrder
){
}
