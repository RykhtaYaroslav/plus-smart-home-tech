package ru.yandex.practicum.order.service;

import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;

public interface OrderOrchestrationService {
    OrderDto processCreating(CreateOrderRequest request);
}
