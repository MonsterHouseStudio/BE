package com.monsterhouse.booking.service;

import com.monsterhouse.booking.repository.BookingDayLockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 날짜 락 행을 "예약 트랜잭션과 분리된 짧은 트랜잭션"에서 미리 만들어 커밋합니다.
 *
 * ★ 왜 별도 트랜잭션이어야 하는가 (데드락 실화)
 *
 * InnoDB 는 존재하지 않는 행에 SELECT ... FOR UPDATE 를 걸면 행 락이 아니라 갭 락을 잡습니다.
 * 갭 락은 서로 호환되므로 N 개 트랜잭션이 동시에 같은 갭을 잠글 수 있습니다.
 * 그 상태에서 각자 INSERT 를 시도하면 insert-intention 락이 남의 갭 락과 충돌하고,
 * 모두가 서로를 기다리며 데드락에 빠집니다.
 *   → 실제로 "겹치지 않는 슬롯 5건 동시 예약" 테스트에서 4건이 Deadlock 으로 죽었습니다.
 *
 * 그래서 순서를 뒤집습니다.
 *   1) 이 클래스가 REQUIRES_NEW 로 INSERT IGNORE 후 즉시 커밋 → 행이 확실히 존재
 *   2) 예약 트랜잭션은 이미 있는 행에 FOR UPDATE → 갭 락 없이 순수 행 락으로 직렬화
 *
 * 여기서 절대 하면 안 되는 것: 호출 전에 FOR UPDATE 로 존재 여부를 먼저 확인하는 것.
 * 그 순간 바깥 트랜잭션이 갭 락을 쥐게 되어, 이 메서드의 INSERT 가 자기 자신을 기다립니다.
 */
@Service
@RequiredArgsConstructor
public class BookingDayLockManager {

    private final BookingDayLockRepository dayLockRepository;

    /**
     * 이미 있으면 아무 일도 하지 않습니다(INSERT IGNORE).
     * 별도 빈으로 분리한 이유: 같은 클래스 안에서 호출하면 프록시를 타지 않아
     * REQUIRES_NEW 가 적용되지 않습니다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensureExists(LocalDate date) {
        dayLockRepository.insertIgnore(date);
    }
}
