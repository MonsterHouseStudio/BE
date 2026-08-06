package com.monsterhouse.content.youtube.dto;

public record YoutubeVideoResponse(
        String videoId,
        String title,
        String thumbnailUrl,
        String publishedAt,
        String watchUrl
) {

    public static YoutubeVideoResponse of(String videoId, String title,
                                          String thumbnailUrl, String publishedAt) {
        return new YoutubeVideoResponse(
                videoId, title, thumbnailUrl, publishedAt,
                "https://www.youtube.com/watch?v=" + videoId);
    }
}
