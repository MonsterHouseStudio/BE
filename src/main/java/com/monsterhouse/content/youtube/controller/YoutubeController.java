package com.monsterhouse.content.youtube.controller;

import com.monsterhouse.common.response.ApiResponse;
import com.monsterhouse.content.youtube.dto.YoutubeVideoResponse;
import com.monsterhouse.content.youtube.service.YoutubeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/youtube")
@RequiredArgsConstructor
public class YoutubeController {

    private final YoutubeService youtubeService;

    /** API 키가 없으면 빈 배열. 프론트는 그때 임베드 링크로 대체합니다. */
    @GetMapping("/latest")
    public ApiResponse<List<YoutubeVideoResponse>> latest() {
        return ApiResponse.ok(youtubeService.latestVideos());
    }
}
