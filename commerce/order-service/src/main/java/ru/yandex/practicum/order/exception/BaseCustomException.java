package ru.yandex.practicum.order.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class BaseCustomException extends RuntimeException {
    private final HttpStatus status;
    private final String description;

    public BaseCustomException(String message, String description, HttpStatus status) {
        super(message);
        this.status = status;
        this.description = description;
    }

    public BaseCustomException(String message, String description, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
        this.description = description;
    }
}
