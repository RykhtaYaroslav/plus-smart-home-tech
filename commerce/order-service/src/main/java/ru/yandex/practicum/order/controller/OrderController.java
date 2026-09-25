package ru.yandex.practicum.order.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.service.OrderOrchestrationService;
import ru.yandex.practicum.order.service.OrderService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
@Validated
public class OrderController {
    private final OrderOrchestrationService orchestrationService;
    private final OrderService orderService;

    @GetMapping
    public List<OrderDto> getAll() {
        return orderService.getAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderDto create(@Valid @RequestBody CreateOrderRequest request) {
        return orchestrationService.processCreating(request);
    }

    @GetMapping("/{id}")
    public OrderDto findById(@PathVariable @Positive Long id) {
        return orderService.findById(id);
    }

    @GetMapping("/by-email")
    public List<OrderDto> findByEmail(@RequestParam String email) {
        return orderService.findByEmail(email);
    }
}
