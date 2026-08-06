package com.monsterhouse.inquiry.service;

import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.common.exception.BusinessException;
import com.monsterhouse.common.exception.ErrorCode;
import com.monsterhouse.inquiry.dto.request.InquiryCreateRequest;
import com.monsterhouse.inquiry.dto.response.AdminInquiryResponse;
import com.monsterhouse.inquiry.dto.response.InquiryCreateResponse;
import com.monsterhouse.inquiry.entity.Inquiry;
import com.monsterhouse.inquiry.entity.InquiryStatus;
import com.monsterhouse.inquiry.entity.InquiryType;
import com.monsterhouse.inquiry.repository.InquiryRepository;
import com.monsterhouse.notification.event.InquiryCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InquiryService {
    private final InquiryRepository inquiryRepository;
    private final ApplicationEventPublisher eventPublisher;
    @Transactional
    public InquiryCreateResponse create(InquiryCreateRequest request, LocaleCode locale){
        if(request.isBot()){
            log.info("Honeypot triggered on inquiry form.");
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
        Inquiry inquiry = Inquiry.builder()
                .type(request.type())
                .name(request.name().trim())
                .contact(request.contact().trim())
                .email(request.email().trim())
                .locale(locale)
                .content(request.content().trim())
                .privacyAgreed(request.privacyAgreed())
                .build();
        inquiryRepository.save(inquiry);
        eventPublisher.publishEvent(new InquiryCreatedEvent(inquiry.getId()));

        log.info("Inquiry created. id={} type={} locale={}",
                inquiry.getId(), inquiry.getType(), locale);

        return new InquiryCreateResponse(inquiry.getId());
    }
    //관리자
    public Page<AdminInquiryResponse> search(InquiryType type, InquiryStatus status, Pageable pageable){
        return inquiryRepository.search(type, status, pageable).map(AdminInquiryResponse::of);
    }
    public long countPending(){
        return inquiryRepository.countByStatus(InquiryStatus.PENDING);
    }
    @Transactional
    public AdminInquiryResponse markHandled(Long inquiryId, String handledBy, String adminMemo){
        Inquiry inquiry = getOrThrow(inquiryId);
        inquiry.markHandled(handledBy, adminMemo);
        return AdminInquiryResponse.of(inquiry);
    }
    @Transactional
    public AdminInquiryResponse markPending(Long inquiryId){
        Inquiry inquiry = getOrThrow(inquiryId);
        inquiry.markPending();
        return AdminInquiryResponse.of(inquiry);
    }
    private Inquiry getOrThrow(Long inquiryId){
        return inquiryRepository.findById(inquiryId).orElseThrow(() -> new BusinessException(ErrorCode.INQUIRY_NOT_FOUND));
    }
}
