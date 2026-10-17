package ru.yandex.practicum.order.dto;

import ru.yandex.practicum.order.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDto(

        Long id,

        String customerName,

        String customerEmail,

        OrderStatus status,

        BigDecimal totalPrice,

        String statusDetails,

        LocalDateTime createdAt,

        List<OrderItemDto> items
) {
}
