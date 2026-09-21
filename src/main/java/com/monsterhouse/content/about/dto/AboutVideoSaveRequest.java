package com.monsterhouse.content.about.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AboutVideoSaveRequest(
        @NotBlank @Size(max = 500) String youtubeUrl,
        @Size(max = 200) String titleKo,
        @Size(max = 200) String titleJa,
        @Size(max = 300) String thumbnailKey,
        boolean active,
        @Min(0) int sortOrder
) {
}
