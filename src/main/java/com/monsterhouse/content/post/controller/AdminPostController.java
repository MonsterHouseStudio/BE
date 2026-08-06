package com.monsterhouse.content.post.controller;

import com.monsterhouse.common.response.ApiResponse;
import com.monsterhouse.content.post.dto.AdminPostResponse;
import com.monsterhouse.content.post.dto.PostSaveRequest;
import com.monsterhouse.content.post.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/posts")
@RequiredArgsConstructor
public class AdminPostController {

    private final PostService postService;

    @GetMapping
    public ApiResponse<List<AdminPostResponse>> list() {
        return ApiResponse.ok(postService.findAllForAdmin());
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminPostResponse> detail(@PathVariable Long id) {
        return ApiResponse.ok(postService.findOne(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AdminPostResponse> create(@Valid @RequestBody PostSaveRequest request) {
        return ApiResponse.ok(postService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<AdminPostResponse> update(@PathVariable Long id,
                                                 @Valid @RequestBody PostSaveRequest request) {
        return ApiResponse.ok(postService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        postService.delete(id);
        return ApiResponse.ok();
    }
}
