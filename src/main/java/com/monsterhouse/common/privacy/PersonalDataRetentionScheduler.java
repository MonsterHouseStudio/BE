package com.monsterhouse.common.privacy;

import com.monsterhouse.admin.repository.RefreshTokenRepository;
import com.monsterhouse.booking.repository.BookingRepository;
import com.monsterhouse.common.config.RetentionProperties;
import com.monsterhouse.inquiry.entity.Inquiry;
import com.monsterhouse.inquiry.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 개인정보 보유기간 경과분 자동 파기 (기획서 §9).
 *
 * "보유기간을 방침에 적어두고 실제로는 계속 갖고 있는" 상태가 가장 위험합니다.
 * 방침 문구와 코드가 같은 값을 보도록 application.yml 한 곳에서 관리합니다.
 *
 * ⚠ 파기 대상은 "완료된" 건뿐입니다.
 *   진행 중인 예약이나 미처리 문의를 날짜만 보고 지우면 운영이 마비됩니다.
 *
 * ⚠ 인스턴스가 여러 대면 같은 시각에 동시에 돌아 중복 실행됩니다.
 *   지금은 단일 인스턴스라 문제없지만, 스케일아웃 시 ShedLock 등이 필요합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.retention.enabled", havingValue = "true", matchIfMissing = true)
public class PersonalDataRetentionScheduler {

    private final BookingRepository bookingRepository;
    private final InquiryRepository inquiryRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final RetentionProperties properties;

    @Scheduled(cron = "${app.retention.cron}", zone = "Asia/Seoul")
    @Transactional
    public void purgeExpired() {
        LocalDateTime now = LocalDateTime.now();

        int bookings = purgeBookings(now);
        int inquiries = purgeInquiries(now);
        int tokens = purgeRefreshTokens(now);

        if (bookings + inquiries + tokens > 0) {
            log.info("개인정보 파기 완료. 예약={}건 문의={}건 리프레시토큰={}건",
                    bookings, inquiries, tokens);
        }
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
}
