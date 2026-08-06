package com.monsterhouse.storage.service;

import com.monsterhouse.common.config.StorageProperties;
import com.monsterhouse.common.exception.BusinessException;
import com.monsterhouse.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * 운영용 S3 저장소 (기획서 §6.2, §7.1).
 *
 * 자격증명은 코드에 넣지 않습니다. DefaultCredentialsProvider 가
 * 환경변수 → 프로파일 → EC2 인스턴스 역할 순으로 알아서 찾습니다.
 * EC2 에 배포하면 인스턴스 역할만 붙여주면 되고 키를 서버에 둘 필요가 없습니다.
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "app.storage.type", havingValue = "s3")
public class S3StorageService implements StorageService {

    private final S3Client s3Client;
    private final StorageProperties properties;

    public S3StorageService(StorageProperties properties) {
        this.properties = properties;
        this.s3Client = S3Client.builder()
                .region(Region.of(properties.region()))
                .build();
        log.info("[s3 storage] bucket={} region={}", properties.bucket(), properties.region());
    }

    @Override
    public void store(String key, byte[] content, String contentType) {
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(properties.bucket())
                            .key(key)
                            .contentType(contentType)
                            // CloudFront 캐시 수명. 키에 해시가 들어가므로 길게 잡아도 안전합니다.
                            .cacheControl("public, max-age=31536000, immutable")
                            .build(),
                    RequestBody.fromBytes(content));
        } catch (S3Exception e) {
            log.error("S3 업로드 실패. key={}", key, e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public void delete(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder()
                    .bucket(properties.bucket())
                    .key(key)
                    .build());
        } catch (S3Exception e) {
            log.warn("S3 삭제 실패. key={}", key, e);
        }
    }

    @Override
    public String url(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        // CloudFront 를 앞에 두면 그 도메인으로, 아니면 S3 직접 URL 로.
        if (properties.cdnDomain() != null && !properties.cdnDomain().isBlank()) {
            return "https://" + properties.cdnDomain() + "/" + key;
        }
        return "https://%s.s3.%s.amazonaws.com/%s"
                .formatted(properties.bucket(), properties.region(), key);
    }
}
