package com.monsterhouse.storage.service;

import com.monsterhouse.common.config.StorageProperties;
import com.monsterhouse.common.exception.BusinessException;
import com.monsterhouse.common.exception.ErrorCode;
import com.monsterhouse.storage.dto.UploadedVideo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoUploadService {

    // ★ MP4 만 받습니다. .mov(video/quicktime)를 빼는 이유:
    //   .mov 도 ftyp 박스를 갖고 있어 위장 검사는 통과하지만, 저장은 .mp4 이름과
    //   video/mp4 헤더로 나갑니다. 내용이 ProRes·HEVC 면 브라우저가 재생하지 못하고
    //   히어로가 검은 화면이 됩니다. 원인을 찾기 매우 어렵습니다.
    //   서버에서 변환하려면 ffmpeg 가 필요하므로, 애초에 거부하고 안내하는 편이 낫습니다.
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("video/mp4");

    private static final byte[] FTYP = {'f', 't', 'y', 'p'};

    private final StorageService storageService;
    private final StorageProperties properties;

    public UploadedVideo upload(MultipartFile file) {
        validate(file);

        String key = buildKey(file);
        try {
            storageService.store(key, file.getBytes(), "video/mp4");
        } catch (IOException e) {
            log.error("영상 업로드 실패. key={}", key, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }

        log.info("배너 영상 업로드 완료. key={} bytes={}", key, file.getSize());
        return new UploadedVideo(key, storageService.url(key), file.getSize(), "video/mp4");
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        long maxBytes = properties.maxVideoBytes();
        if (file.getSize() > maxBytes) {
            // 사용자에게 MB 로 안내합니다. 바이트 숫자는 읽어도 감이 안 옵니다.
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE_VIDEO, maxBytes / (1024 * 1024));
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_VIDEO);
        }

        if (!looksLikeMp4(file)) {
            log.warn("MP4 위장 파일 거부. name={} contentType={}",
                    file.getOriginalFilename(), contentType);
            throw new BusinessException(ErrorCode.UNSUPPORTED_VIDEO);
        }
    }

    private boolean looksLikeMp4(MultipartFile file) {
        try (var in = file.getInputStream()) {
            byte[] head = in.readNBytes(12);
            if (head.length < 12) return false;

            for (int i = 0; i < FTYP.length; i++) {
                if (head[4 + i] != FTYP[i]) return false;
            }
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    private String buildKey(MultipartFile file) {
        String yyyyMM = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"));
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return "banner/%s/%s.mp4".formatted(yyyyMM, uuid);
    }
}