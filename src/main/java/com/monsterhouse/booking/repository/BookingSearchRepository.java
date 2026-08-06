package com.monsterhouse.booking.repository;

import com.monsterhouse.booking.dto.request.BookingSearchCondition;
import com.monsterhouse.booking.entity.Booking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookingSearchRepository {
    Page<Booking> search(BookingSearchCondition condition, Pageable pageable);
}
