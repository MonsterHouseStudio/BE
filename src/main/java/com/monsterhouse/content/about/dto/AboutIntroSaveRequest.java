package com.monsterhouse.content.about.dto;

import jakarta.validation.constraints.Size;

public record AboutIntroSaveRequest(
        @Size(max = 200) String titleKo,
        @Size(max = 200) String titleJa,
        @Size(max = 1000) String descKo,
        @Size(max = 1000) String descJa,
        @Size(max = 300) String photo1Key,
        @Size(max = 300) String photo2Key,
        @Size(max = 300) String photo3Key
) {
}
