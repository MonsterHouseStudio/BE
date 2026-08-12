package com.monsterhouse.notification;

import com.monsterhouse.notification.entity.NotificationOutbox;
import com.monsterhouse.notification.repository.NotificationOutboxRepository;
import com.monsterhouse.notification.sender.NotificationChannel;
import com.monsterhouse.notification.sender.NotificationMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
@Component
@RequiredArgsConstructor
public class OutboxRecorder {
    private final NotificationOutboxRepository outboxRepository;
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long record(NotificationChannel channel, NotificationMessage message) {
        NotificationOutbox saved = outboxRepository.save(NotificationOutbox.builder()
                .channel(channel)
                .recipient(message.to())
                .subject(message.subject())
                .body(message.body())
                .build());
        return saved.getId();
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSent(Long id) {
        outboxRepository.findById(id).ifPresent(NotificationOutbox::markSent);
    }
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long id, String error) {
        outboxRepository.findById(id).ifPresent(o -> o.markFailed(error));
    }
}