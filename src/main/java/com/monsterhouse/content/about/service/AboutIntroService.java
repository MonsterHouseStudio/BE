package com.monsterhouse.content.about.service;

import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.content.about.dto.AboutIntroResponse;
import com.monsterhouse.content.about.dto.AboutIntroSaveRequest;
import com.monsterhouse.content.about.dto.AdminAboutIntroResponse;
import com.monsterhouse.content.about.entity.AboutIntro;
import com.monsterhouse.content.about.repository.AboutIntroRepository;
import com.monsterhouse.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 소개 인트로는 항상 한 행(싱글턴). 없으면 빈 행을 만들어 편집합니다. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AboutIntroService {

    private final AboutIntroRepository repository;
    private final StorageService storageService;

    public AboutIntroResponse findPublic(LocaleCode locale) {
        AboutIntro a = repository.findById(AboutIntro.SINGLETON_ID).orElse(null);
        if (a == null) {
            return new AboutIntroResponse(null, null, null, null, null);
        }
        return AboutIntroResponse.of(a, locale,
                storageService.url(a.getPhoto1Key()),
                storageService.url(a.getPhoto2Key()),
                storageService.url(a.getPhoto3Key()));
    }

    public AdminAboutIntroResponse findForAdmin() {
        AboutIntro a = repository.findById(AboutIntro.SINGLETON_ID).orElse(null);
        if (a == null) {
            return new AdminAboutIntroResponse(null, null, null, null,
                    null, null, null, null, null, null);
        }
        return toAdmin(a);
    }

    @Transactional
    public AdminAboutIntroResponse save(AboutIntroSaveRequest request) {
        AboutIntro a = repository.findById(AboutIntro.SINGLETON_ID).orElse(null);
        if (a == null) {
            a = repository.save(new AboutIntro(
                    request.titleKo(), request.titleJa(), request.descKo(), request.descJa(),
                    emptyToNull(request.photo1Key()), emptyToNull(request.photo2Key()),
                    emptyToNull(request.photo3Key())));
            log.info("소개 인트로 최초 생성");
            return toAdmin(a);
        }

        // 교체된 사진의 이전 파일 정리
        deleteIfReplaced(a.getPhoto1Key(), request.photo1Key());
        deleteIfReplaced(a.getPhoto2Key(), request.photo2Key());
        deleteIfReplaced(a.getPhoto3Key(), request.photo3Key());

        a.update(request.titleKo(), request.titleJa(), request.descKo(), request.descJa(),
                emptyToNull(request.photo1Key()), emptyToNull(request.photo2Key()),
                emptyToNull(request.photo3Key()));
        return toAdmin(a);
    }

    private void deleteIfReplaced(String previousKey, String newKey) {
        if (previousKey != null && !previousKey.equals(emptyToNull(newKey))) {
            storageService.delete(previousKey);
        }
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private AdminAboutIntroResponse toAdmin(AboutIntro a) {
        return AdminAboutIntroResponse.of(a,
                storageService.url(a.getPhoto1Key()),
                storageService.url(a.getPhoto2Key()),
                storageService.url(a.getPhoto3Key()));
    }
}
