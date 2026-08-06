package com.monsterhouse.content.competition.controller;

import com.monsterhouse.common.response.ApiResponse;
import com.monsterhouse.content.competition.dto.AdminCompetitionResponse;
import com.monsterhouse.content.competition.dto.CompetitionSaveRequest;
import com.monsterhouse.content.competition.service.CompetitionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/competitions")
@RequiredArgsConstructor
public class AdminCompetitionController {

    private final CompetitionService competitionService;

    @GetMapping
    public ApiResponse<List<AdminCompetitionResponse>> list() {
        return ApiResponse.ok(competitionService.findAllForAdmin());
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminCompetitionResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(competitionService.findOne(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AdminCompetitionResponse> create(
            @Valid @RequestBody CompetitionSaveRequest request) {
        return ApiResponse.ok(competitionService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminCompetitionResponse> update(
            @PathVariable Long id, @Valid @RequestBody CompetitionSaveRequest request) {
        return ApiResponse.ok(competitionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        competitionService.delete(id);
        return ApiResponse.ok();
    }
}
