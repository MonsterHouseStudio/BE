package com.monsterhouse.content.post.repository;

import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.content.post.entity.Post;
import com.monsterhouse.content.post.entity.PostCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * 공개 목록 — 요청 언어 번역이 없는 글은 제외합니다(기획서 §3.2 폴백 정책).
     *
     * countQuery 를 따로 주는 이유:
     *   join fetch 가 들어간 쿼리는 Spring Data 가 count 쿼리를 자동 생성하지 못합니다.
     */
    @Query(value = """
            select distinct p from Post p
            join fetch p.translations t
            where p.published = true
              and t.locale = :locale
              and (:category is null or p.category = :category)
            order by p.publishedAt desc
            """,
            countQuery = """
                    select count(distinct p) from Post p
                    join p.translations t
                    where p.published = true
                      and t.locale = :locale
                      and (:category is null or p.category = :category)
                    """)
    Page<Post> findPublished(@Param("locale") LocaleCode locale,
                             @Param("category") PostCategory category,
                             Pageable pageable);

    @Query("""
            select p from Post p
            join fetch p.translations t
            where p.slug = :slug
              and p.published = true
              and t.locale = :locale
            """)
    Optional<Post> findPublishedBySlug(@Param("slug") String slug,
                                       @Param("locale") LocaleCode locale);

    @Query("""
            select distinct p from Post p
            left join fetch p.translations
            order by p.createdAt desc
            """)
    List<Post> findAllForAdmin();

    @Query("""
            select p from Post p
            left join fetch p.translations
            where p.id = :id
            """)
    Optional<Post> findByIdWithTranslations(@Param("id") Long id);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    /**
     * 조회수는 엔티티를 불러와 +1 하지 않고 UPDATE 한 방으로 올립니다.
     *
     * 이유: 같은 글을 동시에 여러 명이 보면 read-modify-write 가 서로 덮어써
     * 조회수가 실제보다 적게 쌓입니다(lost update). DB 에서 증가시키면 그 문제가 없습니다.
     */
    @Modifying(clearAutomatically = true)
    @Query("update Post p set p.viewCount = p.viewCount + 1 where p.id = :id")
    void increaseViewCount(@Param("id") Long id);
}
