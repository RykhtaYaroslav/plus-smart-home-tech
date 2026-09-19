package ru.yandex.practicum.order.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.dto.OrderItemDto;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.entity.Order;
import ru.yandex.practicum.order.entity.OrderItem;

@Mapper(componentModel = "spring")
public interface OrderServiceMapper {

    OrderDto toDto(Order order);

    OrderItemDto toDto(OrderItem item);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "status", constant = "CREATED")
    @Mapping(target = "totalPrice", ignore = true)
    @Mapping(target = "statusDetails", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Order toEntity(CreateOrderRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "order", ignore = true)
    OrderItem toEntity(OrderItemRequest request);
}
