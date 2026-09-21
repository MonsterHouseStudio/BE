package com.monsterhouse.content.about.service;

import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.common.exception.BusinessException;
import com.monsterhouse.common.exception.ErrorCode;
import com.monsterhouse.content.about.dto.AdminCrewResponse;
import com.monsterhouse.content.about.dto.CrewResponse;
import com.monsterhouse.content.about.dto.CrewSaveRequest;
import com.monsterhouse.content.about.entity.Crew;
import com.monsterhouse.content.about.repository.CrewRepository;
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
public class CrewService {

    private final CrewRepository repository;
    private final StorageService storageService;

    public List<CrewResponse> findActive(LocaleCode locale) {
        return repository.findByActiveTrueOrderBySortOrderAscIdAsc().stream()
                .map(c -> CrewResponse.of(c, locale, storageService.url(c.getPhotoKey())))
                .toList();
    }

    public List<AdminCrewResponse> findAllForAdmin() {
        return repository.findAllByOrderBySortOrderAscIdAsc().stream().map(this::toAdmin).toList();
    }

    @Transactional
    public AdminCrewResponse create(CrewSaveRequest request) {
        Crew c = repository.save(Crew.builder()
                .nameKo(request.nameKo())
                .nameJa(request.nameJa())
                .roleKo(request.roleKo())
                .roleJa(request.roleJa())
                .bioKo(request.bioKo())
                .bioJa(request.bioJa())
                .photoKey(emptyToNull(request.photoKey()))
                .active(request.active())
                .sortOrder(request.sortOrder())
                .build());
        log.info("크루 생성. id={}", c.getId());
        return toAdmin(c);
    }

    @Transactional
    public AdminCrewResponse update(Long id, CrewSaveRequest request) {
        Crew c = getOrThrow(id);
        String previousKey = c.getPhotoKey();
        boolean photoChanged = previousKey != null && !previousKey.equals(emptyToNull(request.photoKey()));

        c.update(request.nameKo(), request.nameJa(), request.roleKo(), request.roleJa(),
                request.bioKo(), request.bioJa(), emptyToNull(request.photoKey()), request.sortOrder());

        if (photoChanged) {
            storageService.delete(previousKey);
        }
        return toAdmin(c);
    }

    @Transactional
    public AdminCrewResponse setActive(Long id, boolean active) {
        Crew c = getOrThrow(id);
        c.setActive(active);
        return toAdmin(c);
    }

    @Transactional
    public void delete(Long id) {
        Crew c = getOrThrow(id);
        String key = c.getPhotoKey();
        repository.delete(c);
        storageService.delete(key);
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private AdminCrewResponse toAdmin(Crew c) {
        return AdminCrewResponse.of(c, storageService.url(c.getPhotoKey()));
    }

    private Crew getOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
}
