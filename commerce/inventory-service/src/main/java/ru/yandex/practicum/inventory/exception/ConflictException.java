package ru.yandex.practicum.inventory.exception;

import org.springframework.http.HttpStatus;

public class ConflictException extends BaseCustomException {

    public ConflictException(String message) {
        super(message, "Конфликт складской записи", HttpStatus.CONFLICT);
    }
}
