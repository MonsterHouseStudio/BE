package com.monsterhouse.content.stat.repository;

import com.monsterhouse.content.stat.entity.HomeStat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HomeStatRepository extends JpaRepository<HomeStat, Long> {
    List<HomeStat> findByActiveTrueOrderBySortOrderAscIdAsc();
    List<HomeStat> findAllByOrderBySortOrderAscIdAsc();
    long count();
}
