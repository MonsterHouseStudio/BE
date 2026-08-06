package com.monsterhouse.admin.dto;

import com.monsterhouse.booking.dto.response.AdminBookingResponse;
import com.monsterhouse.inquiry.dto.response.AdminInquiryResponse;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSummaryResponse (
        long todayBookings,
        long pendingBookings,
        long pendingInquiries,
        long galleryAwaitingConsent,
        BigDecimal monthRevenue,
        List<AdminBookingResponse> recentBookings,
        List<AdminInquiryResponse> recentInquiries
){
}
