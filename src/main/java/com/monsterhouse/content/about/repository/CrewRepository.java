package com.monsterhouse.content.about.repository;

import com.monsterhouse.content.about.entity.Crew;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CrewRepository extends JpaRepository<Crew, Long> {
    List<Crew> findByActiveTrueOrderBySortOrderAscIdAsc();
    List<Crew> findAllByOrderBySortOrderAscIdAsc();
    long count();
}
