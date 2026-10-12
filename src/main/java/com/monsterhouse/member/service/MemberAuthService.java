package com.monsterhouse.member.service;

import com.monsterhouse.booking.dto.response.BookingResponse;
import com.monsterhouse.booking.repository.BookingRepository;
import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.common.exception.BusinessException;
import com.monsterhouse.common.exception.ErrorCode;
import com.monsterhouse.member.dto.MemberAuthResponse;
import com.monsterhouse.member.dto.MemberLoginRequest;
import com.monsterhouse.member.dto.MemberResponse;
import com.monsterhouse.member.dto.MemberSignupRequest;
import com.monsterhouse.member.entity.Member;
import com.monsterhouse.member.repository.MemberRepository;
import com.monsterhouse.member.security.MemberTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberAuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final MemberTokenProvider tokenProvider;
    private final BookingRepository bookingRepository;

    @Transactional
    public MemberAuthResponse signup(MemberSignupRequest request) {
        String email = request.email().trim().toLowerCase();
        if (memberRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE);
        }
        Member member = memberRepository.save(Member.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(request.password()))
                .name(request.name().trim())
                .phone(request.phone().trim())
                .build());
        log.info("회원 가입. id={}", member.getId());
        return auth(member);
    }

    public MemberAuthResponse login(MemberLoginRequest request) {
        String email = request.email().trim().toLowerCase();
        Member member = memberRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        if (!passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        return auth(member);
    }

    public MemberResponse getMe(Long memberId) {
        return MemberResponse.of(getOrThrow(memberId));
    }

    /** 마이페이지 예약 내역 — 회원 이메일과 일치하는 모든 예약(비회원 때 한 것 포함). */
    public List<BookingResponse> getMyBookings(Long memberId, LocaleCode locale) {
        Member member = getOrThrow(memberId);
        return bookingRepository.findByEmailWithProduct(member.getEmail()).stream()
                .map(b -> BookingResponse.of(b, locale))
                .toList();
    }

    private MemberAuthResponse auth(Member member) {
        return new MemberAuthResponse(
                tokenProvider.createToken(member),
                tokenProvider.expiresInSeconds(),
                MemberResponse.of(member));
    }

    private Member getOrThrow(Long id) {
        return memberRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }
}
