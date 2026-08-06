package com.monsterhouse.notification.sender;

public interface NotificationSender {
    NotificationChannel channel();
    boolean isEnabled();
    void send(NotificationMessage message);
}
