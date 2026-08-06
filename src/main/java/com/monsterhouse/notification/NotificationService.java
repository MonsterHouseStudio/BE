package com.monsterhouse.notification;

import com.monsterhouse.notification.sender.NotificationChannel;
import com.monsterhouse.notification.sender.NotificationMessage;
import com.monsterhouse.notification.sender.NotificationSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 알림 발송의 단일 진입점.
 *
 * 여기서 모든 예외를 삼킵니다. 기획서 §5.3 —
 * "LINE 발송 실패가 신청 자체를 실패시키면 안 된다."
 * 원본 데이터(Booking/Inquiry)는 이미 커밋되어 있고, 알림은 부가 기능입니다.
 */
@Slf4j
@Service
public class NotificationService {

    private final Map<NotificationChannel, NotificationSender> senders =
            new EnumMap<>(NotificationChannel.class);

    public NotificationService(List<NotificationSender> senderList) {
        senderList.forEach(sender -> senders.put(sender.channel(), sender));
    }

    public void send(NotificationChannel channel, NotificationMessage message) {
        NotificationSender sender = senders.get(channel);

        if (sender == null) {
            log.warn("No sender registered for channel {}", channel);
            return;
        }

        if (!sender.isEnabled()) {
            log.info("[{} disabled] subject={} body={}", channel, message.subject(), message.body());
            return;
        }

        try {
            sender.send(message);
        } catch (Exception e) {
            // TODO Phase 5: 실패 건을 notification_failure 테이블에 적재 후 배치 재발송
            log.error("Notification failed. channel={} subject={}", channel, message.subject(), e);
        }
    }

    /** 관리자 알림 이중화 — LINE 이 죽어도 메일은 갑니다 (§5.4 권장 구성). */
    public void notifyAdmin(String subject, String body) {
        send(NotificationChannel.LINE, NotificationMessage.toDefaultRecipient(subject, body));
        send(NotificationChannel.MAIL, NotificationMessage.toDefaultRecipient(subject, body));
    }
}