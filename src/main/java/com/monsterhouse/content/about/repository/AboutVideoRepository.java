package com.monsterhouse.content.about.repository;

import com.monsterhouse.content.about.entity.AboutVideo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AboutVideoRepository extends JpaRepository<AboutVideo, Long> {
    List<AboutVideo> findByActiveTrueOrderBySortOrderAscIdAsc();
    List<AboutVideo> findAllByOrderBySortOrderAscIdAsc();
    long count();
}
