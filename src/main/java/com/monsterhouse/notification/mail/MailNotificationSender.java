package com.monsterhouse.notification.mail;

import com.monsterhouse.common.config.MailNotifyProperties;
import com.monsterhouse.notification.sender.NotificationChannel;
import com.monsterhouse.notification.sender.NotificationMessage;
import com.monsterhouse.notification.sender.NotificationSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MailNotificationSender implements NotificationSender {

    private final JavaMailSender mailSender;
    private final MailNotifyProperties properties;

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.MAIL;
    }

    @Override
    public boolean isEnabled() {
        return properties.enabled();
    }

    @Override
    public void send(NotificationMessage message) {
        String to = (message.to() == null || message.to().isBlank())
                ? properties.adminTo()
                : message.to();

        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(properties.from());
        mail.setTo(to);
        mail.setSubject(message.subject());
        mail.setText(message.body());

        mailSender.send(mail);
        log.info("Mail sent. to={}", maskEmail(to));
    }

    private String maskEmail(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        return at <= 1 ? email : email.charAt(0) + "***" + email.substring(at);
    }
}