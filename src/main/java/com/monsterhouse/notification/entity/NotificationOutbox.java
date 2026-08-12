package com.monsterhouse.notification.entity;

import com.monsterhouse.common.entity.BaseTimeEntity;
import com.monsterhouse.notification.sender.NotificationChannel;
import com.monsterhouse.notification.sender.NotificationMessage;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "notification_outbox", indexes = { @Index(name = "idx_outbox_status_next", columnList = "status, next_attempt_at")})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationOutbox extends BaseTimeEntity{
    public static final int MAX_ATTEMPTS = 5;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;
    @Column(name = "recipient", length = 200)
    private String recipient;
    @Column(name = "subject", length = 300)
    private String subject;
    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;
    @Enumerated(EnumType.STRING )
    @Column(name = "status", nullable = false, length = 20)
    private OutboxStatus status;
    @Column(name = "attempts", nullable = false)
    private int attempts;
    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;
    @Column(name = "last_error", length = 500)
    private String lastError;
    @Column(name = "sent_at")
    private LocalDateTime sentAt;
    @Builder
    private NotificationOutbox(NotificationChannel channel, String recipient, String subject, String body){
        this.channel = channel;
        this.recipient = recipient;
        this.subject = subject;
        this.body = body;
        this.status = OutboxStatus.PENDING;
        this.attempts = 0;
        this.nextAttemptAt = LocalDateTime.now();
    }
    public void markSent(){
        this.status = OutboxStatus.SENT;
        this.sentAt = LocalDateTime.now();
        this.lastError = null;
    }
    public void markFailed(String error){
        this.attempts++;
        this.lastError = error == null ? null : error.substring(0, Math.min(error.length(), 500));
        if(this.attempts >= MAX_ATTEMPTS){
            this.status = OutboxStatus.GIVEN_UP;
            return;
        }
        this.status = OutboxStatus.PENDING;
        this.nextAttemptAt = LocalDateTime.now().plusMinutes(1L << (this.attempts - 1));
    }
    public NotificationMessage toMessage(){
        return new NotificationMessage(recipient, subject, body);
    }

}
