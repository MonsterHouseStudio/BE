package com.monsterhouse.content.banner.service;

import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.common.exception.BusinessException;
import com.monsterhouse.common.exception.ErrorCode;
import com.monsterhouse.content.banner.dto.AdminBannerResponse;
import com.monsterhouse.content.banner.dto.BannerResponse;
import com.monsterhouse.content.banner.dto.BannerSaveRequest;
import com.monsterhouse.content.banner.entity.Banner;
import com.monsterhouse.content.banner.entity.BannerMediaType;
import com.monsterhouse.content.banner.repository.BannerRepository;
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
public class BannerService {
    private final BannerRepository bannerRepository;
    private final StorageService storageService;
    public List<BannerResponse> findActive(LocaleCode locale){
        return bannerRepository.findByActiveTrueOrderBySortOrderAscIdAsc().stream()
                .map(b -> BannerResponse.of(b, locale,
                        storageService.url(b.getMediaKey()),
                        storageService.url(b.getPosterKey())))
                .toList();
    }
    public List<AdminBannerResponse> findAllForAdmin(){
        return bannerRepository.findAllByOrderBySortOrderAscIdAsc().stream().map(this::toAdminResponse).toList();
    }
    @Transactional
    public AdminBannerResponse create(BannerSaveRequest request) {
        validate(request);

        Banner banner = bannerRepository.save(Banner.builder()
                .mediaType(request.mediaType())
                .mediaKey(request.mediaKey())
                .posterKey(emptyToNull(request.posterKey()))
                .headlineKo(request.headlineKo())
                .headlineJa(request.headlineJa())
                .subtextKo(request.subtextKo())
                .subtextJa(request.subtextJa())
                .active(request.active())
                .sortOrder(request.sortOrder())
                .build());

        log.info("배너 생성. id={} type={}", banner.getId(), banner.getMediaType());
        return toAdminResponse(banner);
    }
    @Transactional
    public AdminBannerResponse update(Long id, BannerSaveRequest request){
        validate(request);
        Banner  banner = getOrThrow(id);
        banner.update(
                request.mediaType(),
                request.mediaKey(),
                emptyToNull(request.posterKey()),
                request.headlineKo(),
                request.headlineJa(),
                request.subtextKo(),
                request.subtextJa(),
                request.sortOrder());
        return toAdminResponse(banner);
    }


    @Transactional
    public AdminBannerResponse setActive(Long id, boolean active){
        Banner banner = getOrThrow(id);
        banner.setActive(active);
        return toAdminResponse(banner);
    }
    @Transactional
    public void delete(Long id){
        bannerRepository.delete(getOrThrow(id));
        log.info("배너 삭제. id={}", id);
    }
    private void validate(BannerSaveRequest request){
        if(request.mediaType() == BannerMediaType.VIDEO && (request.posterKey() == null || request.posterKey().isBlank())){
            throw new BusinessException(ErrorCode.BANNER_POSTER_REQUIRED);
        }
    }
    private String emptyToNull(String value){
        return value == null || value.isBlank() ? null : value;
    }
    private AdminBannerResponse toAdminResponse(Banner banner){
        return AdminBannerResponse.of(banner, storageService.url(banner.getMediaKey()), storageService.url(banner.getPosterKey()));
    }
    private Banner getOrThrow(Long id){
        return bannerRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.BANNER_NOT_FOUND));
    }

}
