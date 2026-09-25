package ru.yandex.practicum.order.dto;

import java.math.BigDecimal;

public record OrderItemSnapshot(Long productId, String productName, BigDecimal price, Integer quantity) {
}
