package com.monsterhouse.booking.repository;

import com.monsterhouse.booking.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllByActiveTrueOrderBySortOrderAscIdAsc();

    Optional<Product> findByIdAndActiveTrue(Long id);
}