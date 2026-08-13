package com.monsterhouse.storage.dto;
public record UploadedVideo(
        String key,
        String url,
        long bytes,
        String contentType
) {
}