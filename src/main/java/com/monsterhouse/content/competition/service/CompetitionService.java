package com.monsterhouse.content.competition.service;

import com.monsterhouse.common.enums.LocaleCode;
import com.monsterhouse.common.exception.BusinessException;
import com.monsterhouse.common.exception.ErrorCode;
import com.monsterhouse.content.competition.dto.AdminCompetitionResponse;
import com.monsterhouse.content.competition.dto.CompetitionResponse;
import com.monsterhouse.content.competition.dto.CompetitionSaveRequest;
import com.monsterhouse.content.competition.entity.Competition;
import com.monsterhouse.content.competition.repository.CompetitionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompetitionService {

    private final CompetitionRepository competitionRepository;

    /** 공개 목록 — 요청 언어 번역이 없는 대회는 나오지 않습니다(§3.2). */
    public List<CompetitionResponse> findPublished(LocaleCode locale) {
        return competitionRepository.findPublished(locale).stream()
                .map(c -> CompetitionResponse.of(c, locale))
                .toList();
    }

    public List<AdminCompetitionResponse> findAllForAdmin() {
        return competitionRepository.findAllForAdmin().stream()
                .map(AdminCompetitionResponse::of)
                .toList();
    }

    public AdminCompetitionResponse findOne(Long id) {
        return AdminCompetitionResponse.of(getOrThrow(id));
    }

    @Transactional
    public AdminCompetitionResponse create(CompetitionSaveRequest request) {
        validateDateRange(request);

        Competition competition = Competition.builder()
                .country(request.country())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .link(request.link())
                .published(request.published())
                .build();

        applyTranslations(competition, request);
        competitionRepository.save(competition);

        log.info("Competition created. id={} locales={}",
                competition.getId(), request.translations().size());

        return AdminCompetitionResponse.of(competition);
    }

    @Transactional
    public AdminCompetitionResponse update(Long id, CompetitionSaveRequest request) {
        validateDateRange(request);

        Competition competition = getOrThrow(id);
        competition.update(request.country(), request.startDate(), request.endDate(),
                request.link(), request.published());
        applyTranslations(competition, request);

        return AdminCompetitionResponse.of(competition);
    }

    @Transactional
    public void delete(Long id) {
        competitionRepository.delete(getOrThrow(id));
    }

    // ===================== 내부 =====================

    /**
     * 요청에 없는 언어의 번역은 지웁니다.
     * "일본어를 지웠는데 화면에는 남아 있는" 상태를 만들지 않기 위해
     * 요청 본문을 그 시점의 완전한 상태로 취급합니다.
     */
    private void applyTranslations(Competition competition, CompetitionSaveRequest request) {
        for (LocaleCode locale : LocaleCode.values()) {
            request.translations().stream()
                    .filter(t -> t.locale() == locale)
                    .findFirst()
                    .ifPresentOrElse(
                            t -> competition.putTranslation(
                                    locale, t.name(), t.description(), t.place(), t.host()),
                            () -> competition.removeTranslation(locale)
                    );
        }
    }

    private void validateDateRange(CompetitionSaveRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }
    }

    private Competition getOrThrow(Long id) {
        return competitionRepository.findByIdWithTranslations(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND));
    }
}
