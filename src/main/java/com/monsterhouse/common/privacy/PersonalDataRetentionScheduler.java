package com.monsterhouse.common.privacy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 개인정보 파기 배치의 실행 시점만 담당합니다. 실제 파기는 Service 가 합니다.
 *
 * ★ 트랜잭션을 이 클래스에서 뺀 이유
 *   @Scheduled + @SchedulerLock + @Transactional 을 한 메서드에 겹쳐 놓으면
 *   "락을 잡는 게 먼저냐, 트랜잭션을 여는 게 먼저냐"가 AOP 순서에 의존하게 됩니다.
 *   락은 트랜잭션 바깥에서 잡혀야 합니다 — 트랜잭션 안에서 잡으면
 *   락 획득·해제가 배치 트랜잭션의 커밋/롤백에 끌려다니게 됩니다.
 *   그래서 여기(락)와 Service(트랜잭션)로 층을 갈랐습니다.
 *
 * ★ lockAtMostFor = 10분
 *   이 파드가 배치 도중 죽으면 락을 풀어줄 주체가 없습니다.
 *   10분이 지나면 자동으로 풀려 다음 날 배치가 정상 실행됩니다.
 *   "배치가 아무리 오래 걸려도 이보다는 짧다"는 상한값이어야 합니다.
 *   실제로 이 시간을 넘기면 다른 파드가 끼어들 수 있습니다.
 *
 * ★ lockAtLeastFor = 30초
 *   지울 게 없으면 배치가 0.1초 만에 끝납니다. 그 즉시 락이 풀리는데,
 *   다른 파드의 시계가 조금 늦으면 아직 "새벽 4시 0분"이라 판단해 한 번 더 실행합니다.
 *   최소 30초는 락을 물고 있게 해서 그 창을 닫습니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.retention.enabled", havingValue = "true", matchIfMissing = true)
public class PersonalDataRetentionScheduler {

    private final PersonalDataRetentionService retentionService;

    @Scheduled(cron = "${app.retention.cron}", zone = "Asia/Seoul")
    @SchedulerLock(
            name = "personalDataRetention",
            lockAtLeastFor = "PT30S",
            lockAtMostFor = "PT10M"
    )
    public void purgeExpired() {
        PersonalDataRetentionService.PurgeResult result =
                retentionService.purgeExpired(LocalDateTime.now());

        if (result.total() > 0) {
            log.info("개인정보 파기 완료. 예약={}건 문의={}건 리프레시토큰={}건",
                    result.bookings(), result.inquiries(), result.tokens());
        }
    }
}