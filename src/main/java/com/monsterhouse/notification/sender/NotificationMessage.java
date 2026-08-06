package com.monsterhouse.notification.sender;

public record NotificationMessage(
        /** 메일 주소 또는 LINE userId. null 이면 각 sender 의 기본 수신자. */
        String to,
        /** LINE 은 제목 개념이 없어 무시합니다. */
        String subject,
        String body
) {

    public static NotificationMessage of(String to, String subject, String body) {
        return new NotificationMessage(to, subject, body);
    }

    public static NotificationMessage toDefaultRecipient(String subject, String body) {
        return new NotificationMessage(null, subject, body);
    }
}