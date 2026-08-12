package com.monsterhouse.common.privacy;

import com.monsterhouse.admin.repository.RefreshTokenRepository;
import com.monsterhouse.booking.repository.BookingRepository;
import com.monsterhouse.common.config.RetentionProperties;
import com.monsterhouse.inquiry.entity.Inquiry;
import com.monsterhouse.inquiry.repository.InquiryRepository;
import com.monsterhouse.notification.repository.NotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 개인정보 보유기간 경과분 파기 (기획서 §9).
 *
 * "보유기간을 방침에 적어두고 실제로는 계속 갖고 있는" 상태가 가장 위험합니다.
 * 방침 문구와 코드가 같은 값을 보도록 application.yml 한 곳에서 관리합니다.
 *
 * ⚠ 파기 대상은 "완료된" 건뿐입니다.
 *   진행 중인 예약이나 미처리 문의를 날짜만 보고 지우면 운영이 마비됩니다.
 *
 * now 를 파라미터로 받는 이유: 테스트에서 시간을 고정할 수 있게 하기 위함입니다.
 */
@Service
@RequiredArgsConstructor
public class PersonalDataRetentionService {

    private final BookingRepository bookingRepository;
    private final InquiryRepository inquiryRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final NotificationOutboxRepository outboxRepository;
    private final RetentionProperties properties;

    public record PurgeResult(int bookings, int inquiries, int tokens, int notifications) {
        public int total() {
            return bookings + inquiries + tokens + notifications;
        }
    }

    @Transactional
    public PurgeResult purgeExpired(LocalDateTime now) {
        return new PurgeResult(
                purgeBookings(now),
                purgeInquiries(now),
                purgeRefreshTokens(now),
                purgeSentNotifications(now)
        );
    }

    /** 촬영이 끝난 지 보유기간이 지난 예약. 취소 건도 같은 기준으로 정리합니다. */
    private int purgeBookings(LocalDateTime now) {
        LocalDateTime threshold = now.minusDays(properties.bookingDays());
        return bookingRepository.deleteFinishedBefore(threshold);
    }

    private int purgeInquiries(LocalDateTime now) {
        LocalDateTime threshold = now.minusDays(properties.inquiryDays());
        List<Inquiry> targets = inquiryRepository.findHandledBefore(threshold);

        if (targets.isEmpty()) {
            return 0;
        }
        inquiryRepository.deleteAllInBatch(targets);
        return targets.size();
    }

    /**
     * 만료된 리프레시 토큰은 개인정보는 아니지만 계속 쌓이기만 하는 데이터라
     * 같은 배치에서 함께 정리합니다.
     */
    private int purgeRefreshTokens(LocalDateTime now) {
        return refreshTokenRepository.deleteExpiredBefore(now);
    }
    private int purgeSentNotifications(LocalDateTime now){
        return outboxRepository.deleteSentBefore(now.minusDays(30));
    }
}