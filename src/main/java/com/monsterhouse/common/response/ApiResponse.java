package com.monsterhouse.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        ErrorBody error
) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null, null);
    }

    public static ApiResponse<Void> fail(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorBody(code, message, null));
    }

    public static ApiResponse<Void> fail(String code, String message, List<FieldError> fieldErrors) {
        return new ApiResponse<>(false, null, new ErrorBody(code, message, fieldErrors));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ErrorBody(
            String code,
            String message,
            List<FieldError> fieldErrors
    ) {
    }

    public record FieldError(
            String field,
            String message
    ) {
    }
}