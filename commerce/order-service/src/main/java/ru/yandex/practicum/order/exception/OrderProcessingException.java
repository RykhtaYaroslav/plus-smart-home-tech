package ru.yandex.practicum.order.exception;

import org.springframework.http.HttpStatus;

public class OrderProcessingException extends BaseCustomException {
    public OrderProcessingException(String message) {
        super(message, "Ошибка при оформлении заказа", HttpStatus.UNPROCESSABLE_ENTITY);
    }

}
