package com.monsterhouse.content.gallery.service;

import com.monsterhouse.booking.entity.ProductType;
import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.common.exception.BusinessException;
import com.monsterhouse.common.exception.ErrorCode;
import com.monsterhouse.content.gallery.dto.AdminGalleryItemResponse;
import com.monsterhouse.content.gallery.dto.GalleryItemResponse;
import com.monsterhouse.content.gallery.dto.GallerySaveRequest;
import com.monsterhouse.content.gallery.entity.GalleryItem;
import com.monsterhouse.content.gallery.repository.GalleryItemRepository;
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
public class GalleryService {

    private final GalleryItemRepository galleryItemRepository;
    private final StorageService storageService;

    /** 공개 목록 — 리포지토리에서 consent = true 만 걸러 나옵니다. */
    public List<GalleryItemResponse> findPublic(ProductType category, LocaleCode locale) {
        return galleryItemRepository.findPublic(category).stream()
                .map(item -> GalleryItemResponse.of(item, locale,
                        storageService.url(item.getThumbKey()),
                        storageService.url(item.getImageKey())))
                .toList();
    }

    // ===================== 관리자 =====================

    public List<AdminGalleryItemResponse> findAllForAdmin() {
        return galleryItemRepository.findAllForAdmin().stream()
                .map(this::toAdminResponse)
                .toList();
    }

    /** 대시보드용 — 동의 대기 중인 사진 수 */
    public long countAwaitingConsent() {
        return galleryItemRepository.countByConsentFalse();
    }

    @Transactional
    public AdminGalleryItemResponse create(GallerySaveRequest request) {
        GalleryItem item = GalleryItem.builder()
                .category(request.category())
                .imageKey(request.imageKey())
                .thumbKey(request.thumbKey())
                .ratio(request.ratio())
                .takenAt(request.takenAt())
                .consent(request.consent())
                .consentNote(request.consentNote())
                .sortOrder(request.sortOrder())
                .build();

        applyTranslations(item, request);
        galleryItemRepository.save(item);

        log.info("Gallery item created. id={} consent={}", item.getId(), item.isConsent());

        return toAdminResponse(item);
    }

    @Transactional
    public AdminGalleryItemResponse update(Long id, GallerySaveRequest request) {
        GalleryItem item = getOrThrow(id);

        // 이미지가 교체되면 기존 파일은 고아가 됩니다. 새 키를 적용한 뒤 지웁니다.
        String previousImage = item.getImageKey();
        String previousThumb = item.getThumbKey();
        boolean imageChanged = !previousImage.equals(request.imageKey());

        item.update(request.category(), request.takenAt(), request.sortOrder());
        item.changeConsent(request.consent(), request.consentNote());
        if (imageChanged) {
            item.replaceImage(request.imageKey(), request.thumbKey(), request.ratio());
        }
        applyTranslations(item, request);

        if (imageChanged) {
            storageService.delete(previousImage);
            storageService.delete(previousThumb);
        }

        return toAdminResponse(item);
    }

    /**
     * 동의 상태만 바꾸는 전용 경로.
     *
     * 삭제 요청이 들어왔을 때 사진을 지우기 전에 우선 비공개로 돌려
     * 즉시 노출을 멈출 수 있어야 합니다(기획서 §9).
     */
    @Transactional
    public AdminGalleryItemResponse changeConsent(Long id, boolean consent, String note) {
        GalleryItem item = getOrThrow(id);
        item.changeConsent(consent, note);

        log.info("Gallery consent changed. id={} consent={}", id, consent);

        return toAdminResponse(item);
    }

    @Transactional
    public void delete(Long id) {
        GalleryItem item = getOrThrow(id);
        String imageKey = item.getImageKey();
        String thumbKey = item.getThumbKey();

        galleryItemRepository.delete(item);

        // DB 삭제가 커밋된 뒤에 파일을 지워야 하지만, 여기서는 순서를 바꿔도
        // 최악의 경우 "파일 없는 행"이 아니라 "행 없는 파일"이 남습니다. 후자가 안전합니다.
        storageService.delete(imageKey);
        storageService.delete(thumbKey);
    }

    // ===================== 내부 =====================

    private AdminGalleryItemResponse toAdminResponse(GalleryItem item) {
        return AdminGalleryItemResponse.of(item,
                storageService.url(item.getImageKey()),
                storageService.url(item.getThumbKey()));
    }

    private void applyTranslations(GalleryItem item, GallerySaveRequest request) {
        List<GallerySaveRequest.Translation> given =
                request.translations() == null ? List.of() : request.translations();

        for (LocaleCode locale : LocaleCode.values()) {
            given.stream()
                    .filter(t -> t.locale() == locale)
                    .findFirst()
                    .ifPresentOrElse(
                            t -> item.putTranslation(locale, t.caption()),
                            () -> item.removeTranslation(locale)
                    );
        }
    }

    private GalleryItem getOrThrow(Long id) {
        return galleryItemRepository.findByIdWithTranslations(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
}
