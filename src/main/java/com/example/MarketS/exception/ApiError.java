package com.example.MarketS.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        int status,
        String error,
        String message,
        Instant timestamp,
        Map<String, String> campos
) {
    public static ApiError of(int status, String error, String message) {
        return new ApiError(status, error, message, Instant.now(), null);
    }

    public static ApiError withCampos(int status, String error, String message, Map<String, String> campos) {
        return new ApiError(status, error, message, Instant.now(), campos);
    }
}
