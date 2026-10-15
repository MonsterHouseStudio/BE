package com.monsterhouse.member.controller;

import com.monsterhouse.booking.dto.response.BookingResponse;
import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.common.response.ApiResponse;
import com.monsterhouse.member.dto.AdminMemberResponse;
import com.monsterhouse.member.service.AdminMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/members")
@RequiredArgsConstructor
public class AdminMemberController {

    private final AdminMemberService adminMemberService;

    @GetMapping
    public ApiResponse<List<AdminMemberResponse>> list() {
        return ApiResponse.ok(adminMemberService.findAll());
    }

    @GetMapping("/{id}/bookings")
    public ApiResponse<List<BookingResponse>> bookings(@PathVariable Long id) {
        LocaleCode locale = LocaleCode.from(LocaleContextHolder.getLocale());
        return ApiResponse.ok(adminMemberService.findBookings(id, locale));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        adminMemberService.delete(id);
        return ApiResponse.ok();
    }
}
