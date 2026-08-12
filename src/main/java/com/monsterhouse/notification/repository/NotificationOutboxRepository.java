package com.monsterhouse.notification.repository;

import com.monsterhouse.notification.entity.NotificationOutbox;
import com.monsterhouse.notification.entity.OutboxStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.time.LocalDateTime;
import java.util.List;

public interface NotificationOutboxRepository extends JpaRepository<NotificationOutbox, Long> {
    @Query("""
            select o from NotificationOutbox o
            where o.status = com.monsterhouse.notification.entity.OutboxStatus.PENDING
              and o.nextAttemptAt <= :now
            order by o.nextAttemptAt asc
            """)
    List<NotificationOutbox> findDue(@Param("now")LocalDateTime now, Pageable pageable);
    long countByStatus(OutboxStatus status);
    @Modifying(clearAutomatically = true)
    @Query("""
            delete from NotificationOutbox o
            where o.status = com.monsterhouse.notification.entity.OutboxStatus.SENT
              and o.sentAt < :threshold
            """)
    int deleteSentBefore(@Param("threshold") LocalDateTime threshold);
}
