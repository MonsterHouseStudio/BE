package com.monsterhouse.admin.service;

import com.monsterhouse.admin.dto.DashboardSummaryResponse;
import com.monsterhouse.booking.dto.request.BookingSearchCondition;
import com.monsterhouse.booking.dto.response.AdminBookingResponse;
import com.monsterhouse.booking.entity.BookingStatus;
import com.monsterhouse.booking.repository.BookingRepository;
import com.monsterhouse.content.gallery.service.GalleryService;
import com.monsterhouse.inquiry.entity.InquiryStatus;
import com.monsterhouse.inquiry.service.InquiryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminDashboardService {

    private static final int RECENT_SIZE = 5;

    private final BookingRepository bookingRepository;
    private final InquiryService inquiryService;
    private final GalleryService galleryService;

    public DashboardSummaryResponse summary() {
        LocalDate today = LocalDate.now();
        LocalDateTime dayStart = today.atStartOfDay();
        LocalDateTime dayEnd = today.plusDays(1).atStartOfDay();

        // 이번 달 1일 ~ 다음 달 1일. "지난 30일"이 아니라 달력상의 달입니다.
        // 운영자는 월 단위로 정산하므로 그 기준에 맞춥니다.
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = today.withDayOfMonth(1).plusMonths(1).atStartOfDay();

        BigDecimal revenue = bookingRepository.sumPriceBetween(
                monthStart, monthEnd,
                EnumSet.of(BookingStatus.CONFIRMED, BookingStatus.COMPLETED));

        var recentBookings = bookingRepository
                .search(new BookingSearchCondition(null, null, null, null, null),
                        PageRequest.of(0, RECENT_SIZE))
                .map(AdminBookingResponse::of)
                .getContent();

        var recentInquiries = inquiryService
                .search(null, null, PageRequest.of(0, RECENT_SIZE))
                .getContent();

        return new DashboardSummaryResponse(
                bookingRepository.countByStartAtBetween(dayStart, dayEnd),
                bookingRepository.countByStatus(BookingStatus.REQUESTED),
                inquiryService.countPending(),
                galleryService.countAwaitingConsent(),
                revenue,
                recentBookings,
                recentInquiries
        );
    }
}