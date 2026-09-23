package ru.yandex.practicum.order.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.dto.OrderItemSnapshot;
import ru.yandex.practicum.order.dto.ProductDto;
import ru.yandex.practicum.order.dto.ReserveRequest;
import ru.yandex.practicum.order.dto.ReserveResponse;
import ru.yandex.practicum.order.exception.OrderProcessingException;
import ru.yandex.practicum.order.feign.InventoryClient;
import ru.yandex.practicum.order.feign.ProductClient;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
@Service
@Slf4j
public class OrderOrchestrationServiceImpl implements OrderOrchestrationService {

    private final OrderService orderService;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;

    @Override
    public OrderDto processCreating(CreateOrderRequest request) {
        Map<Long, Integer> itemsAmount = new LinkedHashMap<>();
        List<OrderItemRequest> orderItems = request.items();
        orderItems.forEach(item -> itemsAmount.merge(item.productId(), item.quantity(), Integer::sum));

        Map<Long, ProductDto> products = findItems(itemsAmount.keySet());

        List<ReserveRequest> successfulReservations = reserveItems(itemsAmount);
        try {
            List<OrderItemSnapshot> snapshots = orderItems.stream()
                    .map(item -> {
                        ProductDto product = products.get(item.productId());
                        return new OrderItemSnapshot(product.id(), product.name(), product.price(), item.quantity());
                    }).toList();
            return orderService.create(request, snapshots);
        } catch (RuntimeException exception) {
            releaseReservations(successfulReservations);
            if (exception instanceof OrderProcessingException processingException) {
                throw processingException;
            }
            throw new OrderProcessingException("Не удалось создать заказ");
        }
    }

    private Map<Long, ProductDto> findItems(Set<Long> ids) {
        Map<Long, ProductDto> products = new HashMap<>();

        try {
            for (Long productId : ids) {
                ProductDto product = productClient.getProductById(productId);
                if (!Boolean.TRUE.equals(product.active())) {
                    throw new OrderProcessingException("Товар снят с продажи");
                }
                products.put(productId, product);
            }
            return products;
        } catch (FeignException.NotFound exception) {
            throw new OrderProcessingException("Товар не найден");
        } catch (FeignException exception) {
            throw new OrderProcessingException("Не удалось получить данные товара");
        }
    }

    private List<ReserveRequest> reserveItems(Map<Long, Integer> itemsAmount) {
        List<ReserveRequest> successfulReservations = new ArrayList<>();
        try {
            for (Map.Entry<Long, Integer> item : itemsAmount.entrySet()) {
                ReserveRequest reservation = new ReserveRequest(item.getKey(), item.getValue());
                ReserveResponse response = inventoryClient.reserveStock(reservation);
                if (response == null || !response.success()) {
                    throw new OrderProcessingException("Не удалось зарезервировать товар");
                }
                successfulReservations.add(reservation);
            }
            return successfulReservations;
        } catch (RuntimeException exception) {
            releaseReservations(successfulReservations);
            if (exception instanceof FeignException feignException) {
                throw mapInventoryException(feignException);
            }
            if (exception instanceof OrderProcessingException processingException) {
                throw processingException;
            }
            throw new OrderProcessingException("Не удалось обработать резервирование товара");
        }
    }

    private void releaseReservations(List<ReserveRequest> reservations) {
        for (int i = reservations.size() - 1; i >= 0; i--) {
            ReserveRequest reservation = reservations.get(i);
            try {
                ReserveResponse response = inventoryClient.releaseStock(reservation);
                if (response == null || !response.success()) {
                    log.error("Не удалось снять резерв для productId={} quantity={}",
                            reservation.productId(), reservation.quantity());
                }
            } catch (RuntimeException exception) {
                // Продолжаем компенсацию остальных резервов; исходная ошибка остаётся основной.
                log.error("Ошибка при снятии резерва для productId={} quantity={}",
                        reservation.productId(), reservation.quantity(), exception);
            }
        }
    }

    private OrderProcessingException mapInventoryException(FeignException exception) {
        if (exception.status() == HttpStatus.NOT_FOUND.value()) {
            return new OrderProcessingException("Складская запись не найдена");
        }
        if (exception.status() == HttpStatus.CONFLICT.value()) {
            return new OrderProcessingException("Товара недостаточно");
        }
        return new OrderProcessingException("Не удалось обработать резервирование товара");
    }
}
