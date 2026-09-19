package ru.yandex.practicum.inventory.exception;

import org.springframework.http.HttpStatus;

public class InsufficientStockException extends BaseCustomException {

    public InsufficientStockException(String message) {
        super(message, "Недостаточно товара", HttpStatus.CONFLICT);
    }
}