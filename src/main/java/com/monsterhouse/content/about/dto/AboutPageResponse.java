package com.monsterhouse.content.about.dto;

import java.util.List;

/** 소개 페이지 전체(인트로 + 크루 + 최신 영상)를 한 번에 내려줍니다. */
public record AboutPageResponse(
        AboutIntroResponse intro,
        List<CrewResponse> crew,
        List<AboutVideoResponse> videos
) {
}
