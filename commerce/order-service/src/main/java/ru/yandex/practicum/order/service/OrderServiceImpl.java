package ru.yandex.practicum.order.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.dto.OrderItemSnapshot;
import ru.yandex.practicum.order.entity.Order;
import ru.yandex.practicum.order.entity.OrderItem;
import ru.yandex.practicum.order.exception.NotFoundException;
import ru.yandex.practicum.order.mapper.OrderServiceMapper;
import ru.yandex.practicum.order.repository.OrderRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {
    private final OrderRepository orderRepository;
    private final OrderServiceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> getAll() {
        List<Order> orders = orderRepository.findAll();

        return orders.stream().map(mapper::toDto).toList();
    }

    @Override
    public OrderDto create(CreateOrderRequest request, List<OrderItemSnapshot> items) {
        Order order = mapper.toEntity(request);
        order.setItems(items.stream().map(item -> OrderItem.builder()
                .productId(item.productId())
                .productName(item.productName())
                .price(item.price())
                .quantity(item.quantity())
                .order(order)
                .build()).collect(Collectors.toCollection(ArrayList::new)));
        order.setTotalPrice(order.getItems().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        order.setCreatedAt(LocalDateTime.now());

        orderRepository.save(order);

        return mapper.toDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto findById(Long id) {
        Order order = getOrderOrThrow(id);
        return mapper.toDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> findByEmail(String email) {
        List<Order> orders = orderRepository.findAllByCustomerEmail(email);

        return orders.stream().map(mapper::toDto).toList();
    }

    private Order getOrderOrThrow(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> {
            String m = String.format("Заказ с id = %d не найден", id);
            return new NotFoundException(m);
        });
    }
}
