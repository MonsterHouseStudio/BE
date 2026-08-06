package com.monsterhouse.content.youtube.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.monsterhouse.common.config.YoutubeProperties;
import com.monsterhouse.content.youtube.dto.YoutubeVideoResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 채널 최신 영상 조회 (기획서 §6.3).
 *
 * ★ 캐싱이 선택이 아니라 필수인 이유
 *   YouTube Data API 는 일일 할당량(기본 10,000 units)이 있고
 *   search.list 는 호출당 100 units 를 씁니다. 캐싱 없이 About 페이지 방문마다 호출하면
 *   방문자 100명이면 그날 할당량이 끝납니다.
 *   → 1시간 캐싱하면 하루 24회 = 2,400 units 로 고정됩니다.
 *
 * API 키가 없으면 빈 목록을 돌려줍니다. 프론트는 그때 임베드 링크만 보여주면 되고,
 * 키가 없다고 About 페이지가 통째로 죽지는 않습니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class YoutubeService {

    private static final String SEARCH_URL =
            "https://www.googleapis.com/youtube/v3/search"
                    + "?key=%s&channelId=%s&part=snippet&order=date&type=video&maxResults=%d";

    private final YoutubeProperties properties;

    /** 캐시. 값 하나뿐이라 Map 도 필요 없습니다. */
    private volatile List<YoutubeVideoResponse> cached = List.of();
    private volatile Instant cachedAt = Instant.EPOCH;

    public List<YoutubeVideoResponse> latestVideos() {
        if (!properties.isConfigured()) {
            return List.of();
        }

        if (isCacheFresh()) {
            return cached;
        }

        synchronized (this) {
            // 여러 요청이 동시에 만료를 발견하면 전부 API 를 부릅니다(cache stampede).
            // 락을 잡은 뒤 한 번 더 확인해 한 번만 나가게 합니다.
            if (isCacheFresh()) {
                return cached;
            }
            try {
                cached = fetch();
                cachedAt = Instant.now();
            } catch (Exception e) {
                // 실패해도 낡은 캐시를 그대로 돌려줍니다.
                // About 페이지가 유튜브 장애 때문에 같이 죽을 이유가 없습니다.
                log.warn("YouTube API 호출 실패. 이전 캐시를 사용합니다.", e);
                cachedAt = Instant.now().minus(Duration.ofMinutes(properties.cacheTtlMinutes() - 5L));
            }
            return cached;
        }
    }

    private boolean isCacheFresh() {
        return Duration.between(cachedAt, Instant.now())
                .compareTo(Duration.ofMinutes(properties.cacheTtlMinutes())) < 0;
    }

    private List<YoutubeVideoResponse> fetch() {
        String url = SEARCH_URL.formatted(
                properties.apiKey(), properties.channelId(), properties.maxResults());

        JsonNode root = RestClient.create()
                .get()
                .uri(url)
                .retrieve()
                .body(JsonNode.class);

        List<YoutubeVideoResponse> videos = new ArrayList<>();
        if (root == null || !root.has("items")) {
            return videos;
        }

        for (JsonNode item : root.get("items")) {
            JsonNode id = item.path("id");
            JsonNode snippet = item.path("snippet");

            String videoId = id.path("videoId").asText(null);
            if (videoId == null) {
                continue;
            }

            videos.add(YoutubeVideoResponse.of(
                    videoId,
                    snippet.path("title").asText(""),
                    snippet.path("thumbnails").path("high").path("url").asText(null),
                    snippet.path("publishedAt").asText(null)
            ));
        }

        log.info("YouTube 최신 영상 {}건 갱신", videos.size());
        return videos;
    }
}
