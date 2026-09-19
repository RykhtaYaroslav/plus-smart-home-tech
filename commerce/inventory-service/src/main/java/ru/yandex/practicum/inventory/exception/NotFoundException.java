package ru.yandex.practicum.inventory.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends BaseCustomException {

    public NotFoundException(String message) {
        super(message, "Не удалось найти сущность", HttpStatus.NOT_FOUND);
    }
}