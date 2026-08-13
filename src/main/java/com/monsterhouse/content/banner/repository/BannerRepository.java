package com.monsterhouse.content.banner.repository;

import com.monsterhouse.content.banner.entity.Banner;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BannerRepository extends JpaRepository<Banner, Long> {

    List<Banner> findByActiveTrueOrderBySortOrderAscIdAsc();
    List<Banner> findAllByOrderBySortOrderAscIdAsc();
}