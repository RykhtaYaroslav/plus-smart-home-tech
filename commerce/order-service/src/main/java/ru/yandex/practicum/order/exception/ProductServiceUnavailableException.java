package ru.yandex.practicum.order.exception;

import org.springframework.http.HttpStatus;

public class ProductServiceUnavailableException extends BaseCustomException {
    public ProductServiceUnavailableException(Long productId, Throwable cause) {
        super(
                "Не удалось получить товар с id=" + productId,
                "Сервис товаров временно недоступен",
                HttpStatus.SERVICE_UNAVAILABLE,
                cause
        );
    }
}
