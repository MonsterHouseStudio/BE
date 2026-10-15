package com.monsterhouse.member.service;

import com.monsterhouse.booking.dto.response.BookingResponse;
import com.monsterhouse.booking.repository.BookingRepository;
import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.common.exception.BusinessException;
import com.monsterhouse.common.exception.ErrorCode;
import com.monsterhouse.member.dto.AdminMemberResponse;
import com.monsterhouse.member.entity.Member;
import com.monsterhouse.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/** 관리자용 회원 관리. */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMemberService {

    private final MemberRepository memberRepository;
    private final BookingRepository bookingRepository;

    public List<AdminMemberResponse> findAll() {
        return memberRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .map(AdminMemberResponse::of)
                .toList();
    }

    public List<BookingResponse> findBookings(Long memberId, LocaleCode locale) {
        Member member = getOrThrow(memberId);
        return bookingRepository.findByEmailWithProduct(member.getEmail()).stream()
                .map(b -> BookingResponse.of(b, locale))
                .toList();
    }

    @Transactional
    public void delete(Long memberId) {
        Member member = getOrThrow(memberId);
        memberRepository.delete(member);
        log.info("회원 삭제. id={}", memberId);
    }

    private Member getOrThrow(Long id) {
        return memberRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
}
