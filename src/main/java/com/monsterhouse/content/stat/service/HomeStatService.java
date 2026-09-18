package com.monsterhouse.content.stat.service;

import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.common.exception.BusinessException;
import com.monsterhouse.common.exception.ErrorCode;
import com.monsterhouse.content.stat.dto.AdminHomeStatResponse;
import com.monsterhouse.content.stat.dto.HomeStatResponse;
import com.monsterhouse.content.stat.dto.HomeStatSaveRequest;
import com.monsterhouse.content.stat.entity.HomeStat;
import com.monsterhouse.content.stat.repository.HomeStatRepository;
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
public class HomeStatService {

    private final HomeStatRepository repository;
    private final StorageService storageService;

    public List<HomeStatResponse> findActive(LocaleCode locale) {
        return repository.findByActiveTrueOrderBySortOrderAscIdAsc().stream()
                .map(s -> HomeStatResponse.of(s, locale, storageService.url(s.getPhotoKey())))
                .toList();
    }

    public List<AdminHomeStatResponse> findAllForAdmin() {
        return repository.findAllByOrderBySortOrderAscIdAsc().stream().map(this::toAdmin).toList();
    }

    @Transactional
    public AdminHomeStatResponse create(HomeStatSaveRequest request) {
        validate(request);
        HomeStat s = repository.save(HomeStat.builder()
                .valueNumber(request.valueNumber())
                .suffix(request.suffix())
                .valueText(emptyToNull(request.valueText()))
                .labelKo(request.labelKo())
                .labelJa(request.labelJa())
                .descKo(request.descKo())
                .descJa(request.descJa())
                .photoKey(emptyToNull(request.photoKey()))
                .active(request.active())
                .sortOrder(request.sortOrder())
                .build());
        log.info("홈 통계 생성. id={}", s.getId());
        return toAdmin(s);
    }

    @Transactional
    public AdminHomeStatResponse update(Long id, HomeStatSaveRequest request) {
        validate(request);
        HomeStat s = getOrThrow(id);
        String previousKey = s.getPhotoKey();
        boolean photoChanged = previousKey != null && !previousKey.equals(emptyToNull(request.photoKey()));

        s.update(request.valueNumber(), request.suffix(), emptyToNull(request.valueText()),
                request.labelKo(), request.labelJa(), request.descKo(), request.descJa(),
                emptyToNull(request.photoKey()), request.sortOrder());

        if (photoChanged) {
            storageService.delete(previousKey);
        }
        return toAdmin(s);
    }

    @Transactional
    public AdminHomeStatResponse setActive(Long id, boolean active) {
        HomeStat s = getOrThrow(id);
        s.setActive(active);
        return toAdmin(s);
    }

    @Transactional
    public void delete(Long id) {
        HomeStat s = getOrThrow(id);
        String key = s.getPhotoKey();
        repository.delete(s);
        storageService.delete(key);
    }

    /** valueNumber 와 valueText 중 하나는 있어야 합니다. */
    private void validate(HomeStatSaveRequest request) {
        boolean hasNumber = request.valueNumber() != null;
        boolean hasText = request.valueText() != null && !request.valueText().isBlank();
        if (!hasNumber && !hasText) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private AdminHomeStatResponse toAdmin(HomeStat s) {
        return AdminHomeStatResponse.of(s, storageService.url(s.getPhotoKey()));
    }

    private HomeStat getOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
}
