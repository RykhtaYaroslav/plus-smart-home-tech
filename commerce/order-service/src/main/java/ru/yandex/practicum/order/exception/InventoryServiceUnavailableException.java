package ru.yandex.practicum.order.exception;

import org.springframework.http.HttpStatus;

public class InventoryServiceUnavailableException extends BaseCustomException {
    public InventoryServiceUnavailableException(Long productId, Throwable cause) {
        super(
                "Не удалось выполнить операцию со складом для товара id=" + productId,
                "Складской сервис временно недоступен",
                HttpStatus.SERVICE_UNAVAILABLE,
                cause
        );
    }
}
