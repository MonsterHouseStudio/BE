package com.monsterhouse.notification.event;

import com.monsterhouse.common.config.AsyncConfig;
import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.common.util.MessageUtil;
import com.monsterhouse.inquiry.entity.Inquiry;
import com.monsterhouse.inquiry.entity.InquiryType;
import com.monsterhouse.inquiry.repository.InquiryRepository;
import com.monsterhouse.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 기획서 §5.3 의 흐름을 그대로 구현합니다.
 *
 *   [고객] 폼 작성 → [서버] DB 저장(원본) → [서버] LINE push(관리자) → [관리자] 수동 응대
 *
 * 고객에게 LINE 자동 답장은 하지 않습니다.
 * 친구 추가가 전제이고, 무료 200통이 "수신자 수 × 메시지 수"로 차감되기 때문입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InquiryEventListener {

    /** LINE 텍스트 상한(5000자)과 통수 절약을 고려한 본문 요약 길이 */
    private static final int SUMMARY_LENGTH = 200;

    private final InquiryRepository inquiryRepository;
    private final NotificationService notificationService;
    private final MessageUtil messageUtil;

    @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCreated(InquiryCreatedEvent event) {
        Inquiry inquiry = inquiryRepository.findById(event.inquiryId()).orElse(null);

        if (inquiry == null) {
            log.warn("Inquiry not found for notification. id={}", event.inquiryId());
            return;
        }

        String messageKey = inquiry.getType() == InquiryType.INTERPRETER
                ? "line.inquiry.interpreter"
                : "line.inquiry.video";

        // 관리자 알림은 항상 한국어. 운영자가 한국어 사용자입니다.
        String body = messageUtil.get(messageKey, LocaleCode.KO,
                inquiry.getName(),
                inquiry.getContact(),
                inquiry.summarize(SUMMARY_LENGTH));

        String subject = (inquiry.getType() == InquiryType.INTERPRETER ? "[통역 신청] " : "[영상 문의] ")
                + inquiry.getName();

        // LINE + 메일 이중화 (§5.4). 둘 다 실패해도 여기서 예외가 새어나가지 않습니다.
        notificationService.notifyAdmin(subject, body);
    }
}