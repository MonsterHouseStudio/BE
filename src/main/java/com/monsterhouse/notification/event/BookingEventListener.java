package com.monsterhouse.notification.event;

import com.monsterhouse.booking.entity.Booking;
import com.monsterhouse.booking.entity.BookingStatus;
import com.monsterhouse.booking.repository.BookingRepository;
import com.monsterhouse.common.config.AsyncConfig;
import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.common.util.DateTimeFormatUtil;
import com.monsterhouse.common.util.MessageUtil;
import com.monsterhouse.notification.NotificationService;
import com.monsterhouse.notification.sender.NotificationChannel;
import com.monsterhouse.notification.sender.NotificationMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * ★ 기획서 §4.3 — "알림 발송은 트랜잭션 밖(커밋 후 이벤트)"
 *
 * AFTER_COMMIT: 롤백되면 알림이 나가지 않습니다.
 *               확정 메일을 보내놓고 예약이 롤백되는 사고를 막습니다.
 * @Async:       메일/LINE 응답 지연이 사용자 응답 시간에 섞이지 않게 합니다.
 * REQUIRES_NEW: 원본 트랜잭션은 이미 끝났으므로 조회용 트랜잭션을 새로 엽니다.
 *
 * 고객 메일은 예약 시 선택한 언어(booking.locale)로 보냅니다.
 * 요청 스레드의 LocaleContextHolder 는 여기서 이미 비어 있으니 의존하면 안 됩니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BookingEventListener {

    private final BookingRepository bookingRepository;
    private final NotificationService notificationService;
    private final MessageUtil messageUtil;

    @Async(AsyncConfig.NOTIFICATION_EXECUTOR)
    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleStatusChanged(BookingStatusChangedEvent event) {
        Booking booking = bookingRepository.findById(event.bookingId()).orElse(null);

        if (booking == null) {
            log.warn("Booking not found for notification. id={}", event.bookingId());
            return;
        }

        if (event.isCreation()) {
            notifyCustomer(booking, "mail.booking.requested.subject", "mail.booking.requested.body");
            notifyAdminOfNewBooking(booking);
            return;
        }

        if (event.currentStatus() == BookingStatus.CONFIRMED) {
            notifyCustomer(booking, "mail.booking.confirmed.subject", "mail.booking.confirmed.body");
        } else if (event.currentStatus() == BookingStatus.CANCELED) {
            notifyCustomer(booking, "mail.booking.canceled.subject", "mail.booking.canceled.body");
        }
    }

    private void notifyCustomer(Booking booking, String subjectKey, String bodyKey) {
        LocaleCode locale = booking.getLocale();

        String subject = messageUtil.get(subjectKey, locale);
        String body = String.join("\n\n",
                messageUtil.get("mail.booking.greeting", locale, booking.getName()),
                messageUtil.get(bodyKey, locale),
                String.join("\n",
                        messageUtil.get("mail.booking.info.code", locale, booking.getBookingCode()),
                        messageUtil.get("mail.booking.info.product", locale,
                                booking.getProduct().name(locale)),
                        messageUtil.get("mail.booking.info.datetime", locale,
                                DateTimeFormatUtil.range(
                                        booking.getStartAt(), booking.getEndAt(), locale))),
                messageUtil.get("mail.footer", locale)
        );

        notificationService.send(NotificationChannel.MAIL,
                NotificationMessage.of(booking.getEmail(), subject, body));
    }

    /** 관리자 알림은 항상 한국어. LINE 통수 절약을 위해 1건에 요약해서 보냅니다 (§5.3). */
    private void notifyAdminOfNewBooking(Booking booking) {
        String body = messageUtil.get("line.booking.requested", LocaleCode.KO,
                booking.getProduct().name(LocaleCode.KO),
                DateTimeFormatUtil.range(booking.getStartAt(), booking.getEndAt(), LocaleCode.KO),
                booking.getName(),
                booking.getPhone());

        notificationService.notifyAdmin("[예약 신청] " + booking.getBookingCode(), body);
    }
}