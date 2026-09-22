package ru.yandex.practicum.order.dto;

public record ReserveResponse(
        Long productId,
        Integer reservedQuantity,
        Integer availableQuantity
) {
}