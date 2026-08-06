package com.monsterhouse.content.post.service;

import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.common.exception.BusinessException;
import com.monsterhouse.common.exception.ErrorCode;
import com.monsterhouse.content.post.dto.AdminPostResponse;
import com.monsterhouse.content.post.dto.PostDetailResponse;
import com.monsterhouse.content.post.dto.PostSaveRequest;
import com.monsterhouse.content.post.dto.PostSummaryResponse;
import com.monsterhouse.content.post.entity.Post;
import com.monsterhouse.content.post.entity.PostCategory;
import com.monsterhouse.content.post.repository.PostRepository;
import com.monsterhouse.storage.service.StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final StorageService storageService;

    public Page<PostSummaryResponse> findPublished(LocaleCode locale, PostCategory category,
                                                   Pageable pageable) {
        return postRepository.findPublished(locale, category, pageable)
                .map(p -> PostSummaryResponse.of(p, locale, storageService.url(p.getThumbnailKey())));
    }

    /**
     * 상세 조회. 조회수는 별도 UPDATE 로 올립니다.
     *
     * 조회수 증가 때문에 메서드 전체를 쓰기 트랜잭션으로 만들지 않고
     * increaseViewCount 만 따로 호출하는 이유는 PostRepository 주석 참고.
     */
    @Transactional
    public PostDetailResponse findBySlug(String slug, LocaleCode locale) {
        Post post = postRepository.findPublishedBySlug(slug, locale)
                // 요청 언어 번역이 없으면 "없는 글"로 취급합니다(기획서 §3.2 폴백 정책).
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        postRepository.increaseViewCount(post.getId());

        return PostDetailResponse.of(post, locale, storageService.url(post.getThumbnailKey()));
    }

    // ===================== 관리자 =====================

    public List<AdminPostResponse> findAllForAdmin() {
        return postRepository.findAllForAdmin().stream()
                .map(p -> AdminPostResponse.of(p, storageService.url(p.getThumbnailKey())))
                .toList();
    }

    public AdminPostResponse findOne(Long id) {
        Post post = getOrThrow(id);
        return AdminPostResponse.of(post, storageService.url(post.getThumbnailKey()));
    }

    @Transactional
    public AdminPostResponse create(PostSaveRequest request) {
        if (postRepository.existsBySlug(request.slug())) {
            throw new BusinessException(ErrorCode.DUPLICATE_SLUG);
        }

        Post post = Post.builder()
                .slug(request.slug())
                .category(request.category())
                .thumbnailKey(request.thumbnailKey())
                .published(request.published())
                .build();

        applyTranslations(post, request);
        postRepository.save(post);

        log.info("Post created. id={} slug={}", post.getId(), post.getSlug());

        return AdminPostResponse.of(post, storageService.url(post.getThumbnailKey()));
    }

    @Transactional
    public AdminPostResponse update(Long id, PostSaveRequest request) {
        Post post = getOrThrow(id);

        if (postRepository.existsBySlugAndIdNot(request.slug(), id)) {
            throw new BusinessException(ErrorCode.DUPLICATE_SLUG);
        }

        // 썸네일이 교체되면 기존 파일은 아무도 참조하지 않게 됩니다. 같이 지웁니다.
        String previousKey = post.getThumbnailKey();
        boolean thumbnailChanged = previousKey != null && !previousKey.equals(request.thumbnailKey());

        post.update(request.slug(), request.category(), request.thumbnailKey(), request.published());
        applyTranslations(post, request);

        if (thumbnailChanged) {
            storageService.delete(previousKey);
        }

        return AdminPostResponse.of(post, storageService.url(post.getThumbnailKey()));
    }

    @Transactional
    public void delete(Long id) {
        Post post = getOrThrow(id);
        String key = post.getThumbnailKey();

        postRepository.delete(post);
        storageService.delete(key);
    }

    // ===================== 내부 =====================

    private void applyTranslations(Post post, PostSaveRequest request) {
        for (LocaleCode locale : LocaleCode.values()) {
            request.translations().stream()
                    .filter(t -> t.locale() == locale)
                    .findFirst()
                    .ifPresentOrElse(
                            t -> post.putTranslation(
                                    locale, t.series(), t.title(), t.excerpt(), t.body()),
                            () -> post.removeTranslation(locale)
                    );
        }
    }

    private Post getOrThrow(Long id) {
        return postRepository.findByIdWithTranslations(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
    }
}
