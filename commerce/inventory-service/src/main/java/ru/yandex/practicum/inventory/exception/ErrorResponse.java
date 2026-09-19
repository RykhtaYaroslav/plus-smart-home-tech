package ru.yandex.practicum.inventory.exception;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

@Builder
public record ErrorResponse(

        int status,

        String message,

        LocalDateTime timestamp,

        Map<String, String> validationErrors
) {
    public ErrorResponse(int status, String message) {
        this(status, message, LocalDateTime.now(), null);
    }

    public ErrorResponse(int status, String message, Map<String, String> validationErrors) {
        this(status, message, LocalDateTime.now(), validationErrors);
    }
}