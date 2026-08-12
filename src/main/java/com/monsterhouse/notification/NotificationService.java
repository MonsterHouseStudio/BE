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

    private final Map<NotificationChannel, NotificationSender> senders = new EnumMap<>(NotificationChannel.class);
    private final OutboxRecorder outboxRecorder;
    public NotificationService(List<NotificationSender> senderList, OutboxRecorder outboxRecorder){
        senderList.forEach(sender -> senders.put(sender.channel(), sender));
        this.outboxRecorder = outboxRecorder;
    }
    public void send(NotificationChannel channel, NotificationMessage message){
        NotificationSender sender = senders.get(channel);
        if(sender == null){
            log.warn("No sender registered for channel {}", channel);
            return;
        }
        if(!sender.isEnabled()){
            log.info("[{} disabled] subject={} body={}", channel, message.subject(), message.body());
            return;
        }
        Long outboxId = outboxRecorder.record(channel, message);
        try{
            sender.send(message);
            outboxRecorder.markSent(outboxId);
        }catch(Exception e){
            log.error("Notification failed, will retry. channel={} outboxId={} subject={}",
                    channel, outboxId, message.subject(), e);
            outboxRecorder.markFailed(outboxId, e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }
    public void notifyAdmin(String subject, String body){
        send(NotificationChannel.LINE, NotificationMessage.toDefaultRecipient(subject, body));
        send(NotificationChannel.MAIL, NotificationMessage.toDefaultRecipient(subject, body));
    }
}