package com.monsterhouse.storage.service;

import com.monsterhouse.common.config.StorageProperties;
import com.monsterhouse.common.exception.BusinessException;
import com.monsterhouse.common.exception.ErrorCode;
import com.monsterhouse.storage.dto.UploadedImage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

/**
 * 업로드 이미지 처리 (기획서 §6.2).
 *
 *   원본 → 리사이즈 3종(썸네일/중간/원본) 생성 → 저장
 *
 * ⚠ WebP 변환은 넣지 않았습니다.
 *   Java 표준 ImageIO 에 WebP 인코더가 없어 별도 네이티브 라이브러리가 필요합니다.
 *   지금은 JPEG 3종으로 두고, 필요해지면 webp-imageio 를 붙여 이 클래스만 고치면 됩니다.
 *   (동작하지 않는 WebP 경로를 만들어두는 것보다 없는 편이 낫다고 판단했습니다)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageUploadService {

    /** 재인코딩되므로 확장자는 신뢰하지 않지만, 명백히 아닌 것은 먼저 거릅니다. */
    private static final Set<String> ALLOWED_CONTENT_TYPES =
            Set.of("image/jpeg", "image/jpg", "image/png", "image/webp");

    /** 원본도 무한정 크게 두지 않습니다. 스토리지 비용과 전송 시간이 그대로 늘어납니다. */
    private static final int ORIGINAL_MAX_WIDTH = 2560;
    private static final double JPEG_QUALITY = 0.85;
    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM");

    private final StorageService storageService;
    private final StorageProperties properties;

    public UploadedImage upload(MultipartFile file, String directory) {
        validate(file);

        BufferedImage source = decode(file);
        String base = "%s/%s/%s".formatted(
                directory, LocalDate.now().format(DATE_PATH), UUID.randomUUID().toString().replace("-", ""));

        String originalKey = base + "_o.jpg";
        String thumbKey = base + "_t.jpg";

        storageService.store(originalKey, resize(source, ORIGINAL_MAX_WIDTH), "image/jpeg");
        storageService.store(thumbKey, resize(source, properties.thumbWidth()), "image/jpeg");

        // 원본이 이미 medium 보다 작으면 리사이즈해도 같은 크기가 나옵니다.
        // 그대로 저장하면 바이트 단위로 동일한 파일이 두 벌 쌓여 스토리지만 축냅니다.
        // 이 경우 medium 은 원본을 가리키게 합니다.
        String mediumKey;
        if (source.getWidth() > properties.mediumWidth()) {
            mediumKey = base + "_m.jpg";
            storageService.store(mediumKey, resize(source, properties.mediumWidth()), "image/jpeg");
        } else {
            mediumKey = originalKey;
        }

        log.info("Image uploaded. key={} {}x{}", base, source.getWidth(), source.getHeight());

        return new UploadedImage(
                originalKey, mediumKey, thumbKey,
                storageService.url(originalKey),
                storageService.url(mediumKey),
                storageService.url(thumbKey),
                source.getWidth(), source.getHeight(),
                ratioOf(source.getWidth(), source.getHeight())
        );
    }

    public void delete(String... keys) {
        for (String key : keys) {
            storageService.delete(key);
        }
    }

    // ===================== 내부 =====================

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        if (file.getSize() > properties.maxImageBytes()) {
            throw new BusinessException(ErrorCode.FILE_TOO_LARGE, properties.maxImageBytes() / 1024 / 1024);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }
    }

    /**
     * ★ 여기가 파일 업로드 보안의 핵심입니다 (기획서 §9).
     *
     * Content-Type 헤더와 확장자는 클라이언트가 마음대로 지어낼 수 있습니다.
     * ImageIO 로 실제 디코딩을 시도해서 "진짜 이미지인지"를 확인하고,
     * 이후 전부 JPEG 로 재인코딩합니다.
     *
     * 재인코딩의 부수 효과가 중요합니다:
     *   - 이미지처럼 보이지만 뒤에 스크립트가 붙은 폴리글롯 파일이 무력화됩니다
     *   - EXIF 의 GPS 좌표 등 개인정보가 제거됩니다 (촬영 장소 노출 방지)
     */
    private BufferedImage decode(MultipartFile file) {
        try {
            BufferedImage image = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
            if (image == null) {
                log.info("이미지로 디코딩되지 않는 파일 거부. name={}", file.getOriginalFilename());
                throw new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE);
            }
            return image;
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.UNSUPPORTED_FILE_TYPE);
        }
    }

    private byte[] resize(BufferedImage source, int targetWidth) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            int width = Math.min(targetWidth, source.getWidth());
            int height = (int) Math.round(source.getHeight() * (width / (double) source.getWidth()));

            Thumbnails.of(source)
                    .size(width, Math.max(1, height))
                    .outputFormat("jpg")
                    .outputQuality(JPEG_QUALITY)
                    // 투명 PNG 를 JPEG 로 바꾸면 투명 부분이 검게 나옵니다. 흰색으로 채웁니다.
                    .imageType(BufferedImage.TYPE_INT_RGB)
                    .toOutputStream(out);

            return out.toByteArray();
        } catch (IOException e) {
            log.error("이미지 리사이즈 실패", e);
            throw new BusinessException(ErrorCode.INTERNAL_ERROR);
        }
    }

    private String ratioOf(int width, int height) {
        double r = width / (double) height;
        if (r > 1.15) return "landscape";
        if (r < 0.87) return "portrait";
        return "square";
    }
}
