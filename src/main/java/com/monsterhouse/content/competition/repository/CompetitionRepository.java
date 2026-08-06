package com.monsterhouse.content.competition.repository;

import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.content.competition.entity.Competition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CompetitionRepository extends JpaRepository<Competition, Long> {

    /**
     * 공개 목록 — 기획서 §3.2 폴백 정책대로 요청 언어의 번역이 없는 대회는 제외합니다.
     *
     * join fetch 로 번역을 함께 가져오는 이유:
     *   응답을 만들 때 대회마다 번역을 다시 조회하면 N+1 이 됩니다.
     *   여기서는 locale 로 이미 한 건만 걸러지므로 페이징 없이 fetch join 해도 안전합니다.
     */
    @Query("""
            select distinct c from Competition c
            join fetch c.translations t
            where c.published = true
              and t.locale = :locale
            order by c.startDate asc
            """)
    List<Competition> findPublished(@Param("locale") LocaleCode locale);

    /** 관리자 목록 — 번역 유무와 무관하게 전부. 미번역 배지를 보여줘야 하므로 전체 번역을 붙입니다. */
    @Query("""
            select distinct c from Competition c
            left join fetch c.translations
            order by c.startDate desc
            """)
    List<Competition> findAllForAdmin();

    @Query("""
            select c from Competition c
            left join fetch c.translations
            where c.id = :id
            """)
    Optional<Competition> findByIdWithTranslations(@Param("id") Long id);
}
