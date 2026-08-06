package com.monsterhouse.storage.controller;

import com.monsterhouse.common.exception.BusinessException;
import com.monsterhouse.common.exception.ErrorCode;
import com.monsterhouse.common.response.ApiResponse;
import com.monsterhouse.storage.dto.UploadedImage;
import com.monsterhouse.storage.service.ImageUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

/**
 * 이미지 업로드는 관리자만 가능합니다.
 * 공개 엔드포인트로 열면 서버가 무료 이미지 호스팅이 됩니다.
 */
@RestController
@RequestMapping("/api/admin/uploads")
@RequiredArgsConstructor
public class AdminUploadController {

    /** 임의의 경로를 받으면 저장소 아무 데나 쓸 수 있으므로 화이트리스트로 제한합니다. */
    private static final Set<String> ALLOWED_DIRECTORIES = Set.of("gallery", "post", "product");

    private final ImageUploadService imageUploadService;

    @PostMapping("/images")
    public ApiResponse<UploadedImage> uploadImage(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "gallery") String directory) {

        if (!ALLOWED_DIRECTORIES.contains(directory)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        return ApiResponse.ok(imageUploadService.upload(file, directory));
    }
}
