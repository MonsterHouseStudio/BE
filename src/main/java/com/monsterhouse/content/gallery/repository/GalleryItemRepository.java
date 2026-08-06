package com.monsterhouse.content.gallery.repository;

import com.monsterhouse.booking.entity.ProductType;
import com.monsterhouse.content.gallery.entity.GalleryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GalleryItemRepository extends JpaRepository<GalleryItem, Long> {

    /**
     * 공개 목록 — consent = true 인 사진만 나갑니다 (기획서 §6.2 초상권).
     * 이 조건을 서비스가 아니라 쿼리에 박아둔 이유:
     *   서비스 어딘가에서 필터를 빠뜨리면 동의 없는 사진이 그대로 노출됩니다.
     *   되돌릴 수 없는 사고이므로 데이터 접근 지점에서 막습니다.
     */
    @Query("""
            select distinct g from GalleryItem g
            left join fetch g.translations
            where g.consent = true
              and (:category is null or g.category = :category)
            order by g.sortOrder asc, g.takenAt desc, g.id desc
            """)
    List<GalleryItem> findPublic(@Param("category") ProductType category);

    @Query("""
            select distinct g from GalleryItem g
            left join fetch g.translations
            order by g.sortOrder asc, g.id desc
            """)
    List<GalleryItem> findAllForAdmin();

    @Query("""
            select g from GalleryItem g
            left join fetch g.translations
            where g.id = :id
            """)
    Optional<GalleryItem> findByIdWithTranslations(@Param("id") Long id);

    long countByConsentFalse();
}
