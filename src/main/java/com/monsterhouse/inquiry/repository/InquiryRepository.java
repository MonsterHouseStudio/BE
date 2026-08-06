package com.monsterhouse.inquiry.repository;

import com.monsterhouse.inquiry.entity.Inquiry;
import com.monsterhouse.inquiry.entity.InquiryStatus;
import com.monsterhouse.inquiry.entity.InquiryType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {

    /**
     * type/status 는 조합이 4가지뿐이라 QueryDSL 까지 갈 필요가 없습니다.
     * null 을 "조건 없음"으로 처리하는 JPQL 로 충분합니다.
     */
    @Query("""
            select i from Inquiry i
            where (:type is null or i.type = :type)
              and (:status is null or i.status = :status)
            order by i.createdAt desc
            """)
    Page<Inquiry> search(@Param("type") InquiryType type,
                         @Param("status") InquiryStatus status,
                         Pageable pageable);

    long countByStatus(InquiryStatus status);

    /**
     * 개인정보 보유기간 경과분 파기용 (기획서 §9 — 문의 내역은 처리 완료일로부터 6개월).
     * 배치는 F단계에서 붙입니다.
     */
    @Query("select i from Inquiry i where i.handledAt is not null and i.handledAt < :threshold")
    java.util.List<Inquiry> findHandledBefore(@Param("threshold") LocalDateTime threshold);
}