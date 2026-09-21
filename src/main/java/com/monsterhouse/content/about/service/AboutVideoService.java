package com.monsterhouse.content.about.service;

import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.common.exception.BusinessException;
import com.monsterhouse.common.exception.ErrorCode;
import com.monsterhouse.content.about.dto.AboutVideoResponse;
import com.monsterhouse.content.about.dto.AboutVideoSaveRequest;
import com.monsterhouse.content.about.dto.AdminAboutVideoResponse;
import com.monsterhouse.content.about.entity.AboutVideo;
import com.monsterhouse.content.about.repository.AboutVideoRepository;
import com.monsterhouse.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AboutVideoService {

    private final AboutVideoRepository repository;
    private final StorageService storageService;

    public List<AboutVideoResponse> findActive(LocaleCode locale) {
        return repository.findByActiveTrueOrderBySortOrderAscIdAsc().stream()
                .map(v -> AboutVideoResponse.of(v, locale, storageService.url(v.getThumbnailKey())))
                .toList();
    }

    public List<AdminAboutVideoResponse> findAllForAdmin() {
        return repository.findAllByOrderBySortOrderAscIdAsc().stream().map(this::toAdmin).toList();
    }

    @Transactional
    public AdminAboutVideoResponse create(AboutVideoSaveRequest request) {
        AboutVideo v = repository.save(AboutVideo.builder()
                .youtubeUrl(request.youtubeUrl().trim())
                .titleKo(request.titleKo())
                .titleJa(request.titleJa())
                .thumbnailKey(emptyToNull(request.thumbnailKey()))
                .active(request.active())
                .sortOrder(request.sortOrder())
                .build());
        log.info("소개 영상 생성. id={}", v.getId());
        return toAdmin(v);
    }

    @Transactional
    public AdminAboutVideoResponse update(Long id, AboutVideoSaveRequest request) {
        AboutVideo v = getOrThrow(id);
        String previousKey = v.getThumbnailKey();
        boolean thumbChanged = previousKey != null && !previousKey.equals(emptyToNull(request.thumbnailKey()));

        v.update(request.youtubeUrl().trim(), request.titleKo(), request.titleJa(),
                emptyToNull(request.thumbnailKey()), request.sortOrder());

        if (thumbChanged) {
            storageService.delete(previousKey);
        }
        return toAdmin(v);
    }

    @Transactional
    public AdminAboutVideoResponse setActive(Long id, boolean active) {
        AboutVideo v = getOrThrow(id);
        v.setActive(active);
        return toAdmin(v);
    }

    @Transactional
    public void delete(Long id) {
        AboutVideo v = getOrThrow(id);
        String key = v.getThumbnailKey();
        repository.delete(v);
        storageService.delete(key);
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private AdminAboutVideoResponse toAdmin(AboutVideo v) {
        return AdminAboutVideoResponse.of(v, storageService.url(v.getThumbnailKey()));
    }

    private AboutVideo getOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
}
