package ru.yandex.practicum.order.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.order.OrderStatus;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.dto.OrderItemSnapshot;
import ru.yandex.practicum.order.dto.ProductDto;
import ru.yandex.practicum.order.dto.ReserveRequest;
import ru.yandex.practicum.order.dto.ReserveResponse;
import ru.yandex.practicum.order.exception.InventoryServiceUnavailableException;
import ru.yandex.practicum.order.exception.OrderProcessingException;
import ru.yandex.practicum.order.exception.ProductServiceUnavailableException;
import ru.yandex.practicum.order.feign.InventoryClient;
import ru.yandex.practicum.order.feign.ProductClient;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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

    /** Выполняет оформление заказа: получает данные товаров, резервирует остатки и сохраняет заказ. */
    @Override
    public OrderDto processCreating(CreateOrderRequest request) {
        Map<Long, Integer> itemQuantities = aggregateItemQuantities(request.items());
        ProductLookupResult productLookup = lookupProducts(itemQuantities.keySet());
        ReservationResult reservation = reserveItemsOrMarkPending(itemQuantities);
        return saveOrderWithReservationCompensation(request, productLookup, reservation);
    }

    /** Объединяет количества повторяющихся товаров в заказе по их идентификаторам. */
    private Map<Long, Integer> aggregateItemQuantities(List<OrderItemRequest> items) {
        Map<Long, Integer> itemQuantities = new LinkedHashMap<>();
        items.forEach(item -> itemQuantities.merge(item.productId(), item.quantity(), Integer::sum));
        return itemQuantities;
    }

    /** Загружает данные товаров и отмечает идентификаторы, для которых каталог недоступен. */
    private ProductLookupResult lookupProducts(Set<Long> productIds) {
        Map<Long, ProductDto> products = new HashMap<>();
        Set<Long> unavailableProductIds = new LinkedHashSet<>();

        for (Long productId : productIds) {
            try {
                ProductDto product = productClient.getProductById(productId);
                if (!Boolean.TRUE.equals(product.active())) {
                    throw new OrderProcessingException("Товар снят с продажи");
                }
                products.put(productId, product);
            } catch (ProductServiceUnavailableException exception) {
                unavailableProductIds.add(productId);
                log.warn("Каталог недоступен, товар id={} будет сохранён для ручной проверки", productId, exception);
            } catch (FeignException.NotFound exception) {
                throw new OrderProcessingException("Товар не найден");
            } catch (FeignException exception) {
                throw new OrderProcessingException("Не удалось получить данные товара");
            }
        }
        return new ProductLookupResult(products, unavailableProductIds);
    }

    /** Пытается зарезервировать товары; при технической недоступности помечает заказ ожидающим подтверждения. */
    private ReservationResult reserveItemsOrMarkPending(Map<Long, Integer> itemQuantities) {
        try {
            return new ReservationResult(reserveItems(itemQuantities), false);
        } catch (InventoryServiceUnavailableException exception) {
            return new ReservationResult(List.of(), true);
        }
    }

    /** Создаёт снимки позиций и подставляет заглушку только для товаров с подтверждённой недоступностью каталога. */
    private List<OrderItemSnapshot> createItemSnapshots(
            List<OrderItemRequest> orderItems,
            ProductLookupResult productLookup
    ) {
        return orderItems.stream()
                .map(item -> {
                    if (productLookup.unavailableProductIds().contains(item.productId())) {
                        return new OrderItemSnapshot(
                                item.productId(),
                                "Товар #" + item.productId() + " (ожидает проверки)",
                                BigDecimal.ZERO,
                                item.quantity()
                        );
                    }
                    ProductDto product = productLookup.products().get(item.productId());
                    if (product == null) {
                        throw new OrderProcessingException("Не удалось получить данные товара");
                    }
                    return new OrderItemSnapshot(product.id(), product.name(), product.price(), item.quantity());
                }).toList();
    }

    /** Выбирает статус CONFIRMED или PENDING_CONFIRMATION по результатам каталога и резервирования. */
    private OrderStatus determineOrderStatus(ProductLookupResult productLookup, ReservationResult reservation) {
        boolean requiresConfirmation = reservation.unavailable()
                || !productLookup.unavailableProductIds().isEmpty();
        return requiresConfirmation ? OrderStatus.PENDING_CONFIRMATION : OrderStatus.CONFIRMED;
    }

    /** Сохраняет заказ и при ошибке сохранения пытается снять ранее созданные резервы. */
    private OrderDto saveOrderWithReservationCompensation(
            CreateOrderRequest request,
            ProductLookupResult productLookup,
            ReservationResult reservation
    ) {
        try {
            List<OrderItemSnapshot> itemSnapshots = createItemSnapshots(request.items(), productLookup);
            OrderStatus status = determineOrderStatus(productLookup, reservation);
            String statusDetails = createStatusDetails(
                    productLookup.unavailableProductIds(),
                    reservation.unavailable()
            );
            return orderService.create(request, itemSnapshots, status, statusDetails);
        } catch (RuntimeException exception) {
            releaseReservations(reservation.successfulReservations());
            if (exception instanceof OrderProcessingException processingException) {
                throw processingException;
            }
            throw new OrderProcessingException("Не удалось создать заказ");
        }
    }

    /** Резервирует все позиции и компенсирует уже созданные резервы при ошибке. */
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
            if (exception instanceof InventoryServiceUnavailableException serviceUnavailableException) {
                throw serviceUnavailableException;
            }
            if (exception instanceof OrderProcessingException processingException) {
                throw processingException;
            }
            throw new OrderProcessingException("Не удалось обработать резервирование товара");
        }
    }

    /** Снимает резервы в обратном порядке, логируя ошибки и продолжая остальные попытки. */
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

    /** Преобразует бизнес-ошибки inventory-service в сообщения для сценария оформления заказа. */
    private OrderProcessingException mapInventoryException(FeignException exception) {
        if (exception.status() == HttpStatus.NOT_FOUND.value()) {
            return new OrderProcessingException("Складская запись не найдена");
        }
        if (exception.status() == HttpStatus.CONFLICT.value()) {
            return new OrderProcessingException("Товара недостаточно");
        }
        return new OrderProcessingException("Не удалось обработать резервирование товара");
    }

    /** Формирует пояснение статуса заказа для случаев, требующих ручной проверки. */
    private String createStatusDetails(Set<Long> unavailableProductIds, boolean inventoryUnavailable) {
        List<String> details = new ArrayList<>();
        if (!unavailableProductIds.isEmpty()) {
            details.add("Нет данных каталога для товаров: " + unavailableProductIds);
        }
        if (inventoryUnavailable) {
            details.add("Резервирование на складе не подтверждено");
        }
        return details.isEmpty() ? null : "Требуется ручная проверка: " + String.join("; ", details) + ".";
    }

    private record ProductLookupResult(Map<Long, ProductDto> products, Set<Long> unavailableProductIds) {
    }

    private record ReservationResult(List<ReserveRequest> successfulReservations, boolean unavailable) {
    }
}
