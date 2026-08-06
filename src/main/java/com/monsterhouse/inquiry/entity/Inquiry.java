package com.monsterhouse.inquiry.entity;

import com.monsterhouse.common.entity.BaseTimeEntity;
import com.monsterhouse.common.enums.LocaleCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 통역 신청과 영상 문의를 type 으로 구분해 한 테이블에 담습니다 (기획서 §6.5).
 *
 * 이게 원본입니다. LINE 발송이 실패해도 이 행은 남아야 하고,
 * 관리자는 LINE 알림이 아니라 이 테이블을 보고 처리합니다.
 */
@Getter
@Entity
@Table(
        name = "inquiry",
        indexes = {
                @Index(name = "idx_inquiry_status_created", columnList = "status, created_at"),
                @Index(name = "idx_inquiry_type", columnList = "type")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Inquiry extends BaseTimeEntity{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private InquiryType type;
    @Column(name = "name", nullable = false, length = 50)
    private String name;
    @Column(name = "contact", nullable =false, length = 100)
    private String contact;
    @Column(name = "email", nullable = false, length = 200)
    private  String email;
    @Enumerated(EnumType.STRING)
    @Column(name = "locale", nullable =false, length = 5)
    private LocaleCode locale;
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InquiryStatus status;
    @Column(name = "privacy_agreed", nullable = false)
    private boolean privacyAgreed;
    @Column(name = "handled_at")
    private LocalDateTime handledAt;
    @Column(name = "handled_by", length = 50)
    private String handledBy;
    @Column(name = "admin_memo", length = 500)
    private String adminMemo;
    @Builder
    private Inquiry(InquiryType type, String name, String contact, String email, LocaleCode locale, String content, boolean privacyAgreed){
        this.type = type;
        this.name = name;
        this.contact = contact;
        this.email = email;
        this.locale = locale;
        this.content = content;
        this.privacyAgreed = privacyAgreed;
        this.status = InquiryStatus.PENDING;
    }
    public void markHandled(String handledBy, String adminMemo){
        this.status = InquiryStatus.HANDLED;
        this.handledAt = LocalDateTime.now();
        this.handledBy = handledBy;
        this.adminMemo = adminMemo;
    }
    public void markPending(){
        this.status = InquiryStatus.PENDING;
        this.handledAt = null;
        this.handledBy = null;
    }
    public String summarize(int maxLength){
        String flat = content.replaceAll("\\s+", " ").trim();
        return flat.length() <= maxLength ? flat : flat.substring(0, maxLength) + "...";
    }
}
