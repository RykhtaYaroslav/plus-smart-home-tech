package ru.yandex.practicum.order.service;

import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.dto.OrderItemSnapshot;

import java.util.List;

public interface OrderService {
    List<OrderDto> getAll();

    OrderDto create(CreateOrderRequest request, List<OrderItemSnapshot> items);

    OrderDto findById(Long id);

    List<OrderDto> findByEmail(String email);
}
