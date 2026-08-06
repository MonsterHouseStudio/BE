package com.monsterhouse.storage.service;

import com.monsterhouse.common.config.StorageProperties;
import com.monsterhouse.common.exception.BusinessException;
import com.monsterhouse.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 개발용 로컬 디스크 저장소.
 * WebConfig 의 정적 리소스 매핑을 통해 /uploads/** 로 서빙됩니다.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private final Path root;

    public LocalStorageService(StorageProperties properties) {
        this.root = Paths.get(properties.localPath()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
            log.info("[local storage] 업로드 경로: {}", root);
        } catch (IOException e) {
            throw new IllegalStateException("업로드 디렉터리를 만들 수 없습니다: " + root, e);
        }
    }

    @Override
    public void store(String key, byte[] content, String contentType) {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            log.error("파일 저장 실패. key={}", key, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public void delete(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException e) {
            // 삭제 실패가 사용자 요청을 실패시킬 이유는 없습니다. 고아 파일은 나중에 정리합니다.
            log.warn("파일 삭제 실패. key={}", key, e);
        }
    }

    @Override
    public String url(String key) {
        return (key == null || key.isBlank()) ? null : "/uploads/" + key;
    }

    /**
     * key 에 ../ 가 섞여 들어오면 업로드 디렉터리 밖에 파일을 쓰거나 지울 수 있습니다.
     * 경로 탈출(path traversal)을 여기서 차단합니다.
     */
    private Path resolve(String key) {
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) {
            log.warn("경로 탈출 시도 차단. key={}", key);
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return target;
    }
}
