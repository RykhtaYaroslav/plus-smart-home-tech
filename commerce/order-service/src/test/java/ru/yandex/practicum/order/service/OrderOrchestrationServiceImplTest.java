package ru.yandex.practicum.order.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.order.OrderStatus;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderOrchestrationServiceImplTest {

    @Mock
    private OrderService orderService;

    @Mock
    private ProductClient productClient;

    @Mock
    private InventoryClient inventoryClient;

    private OrderOrchestrationServiceImpl orchestrationService;

    @BeforeEach
    void setUp() {
        orchestrationService = new OrderOrchestrationServiceImpl(orderService, productClient, inventoryClient);
    }

    @Test
    void successfulOrderIsSavedAsConfirmed() {
        CreateOrderRequest request = createRequest(new OrderItemRequest(10L, 2));
        stubProduct(10L);
        when(inventoryClient.reserveStock(new ReserveRequest(10L, 2)))
                .thenReturn(new ReserveResponse(true, 8, "Зарезервировано"));

        orchestrationService.processCreating(request);

        verify(orderService).create(
                eq(request),
                anyList(),
                eq(OrderStatus.CONFIRMED),
                isNull()
        );
    }

    @Test
    void reservationBusinessFailureDoesNotSaveOrderAndCompensatesEarlierReservations() {
        CreateOrderRequest request = createRequest(
                new OrderItemRequest(10L, 1),
                new OrderItemRequest(20L, 1)
        );
        stubProduct(10L);
        stubProduct(20L);
        when(inventoryClient.reserveStock(new ReserveRequest(10L, 1)))
                .thenReturn(new ReserveResponse(true, 9, "Зарезервировано"));
        when(inventoryClient.reserveStock(new ReserveRequest(20L, 1)))
                .thenReturn(new ReserveResponse(false, 0, "Недостаточно товара"));
        when(inventoryClient.releaseStock(new ReserveRequest(10L, 1)))
                .thenReturn(new ReserveResponse(true, 10, "Резерв снят"));

        assertThrows(OrderProcessingException.class, () -> orchestrationService.processCreating(request));

        verify(orderService, never()).create(any(), anyList(), any(), any());
        verify(inventoryClient).releaseStock(new ReserveRequest(10L, 1));
    }

    @Test
    void unavailableProductServiceSavesPlaceholderAndPendingOrder() {
        CreateOrderRequest request = createRequest(new OrderItemRequest(10L, 2));
        when(productClient.getProductById(10L))
                .thenThrow(new ProductServiceUnavailableException(10L, new RuntimeException("Каталог недоступен")));
        when(inventoryClient.reserveStock(new ReserveRequest(10L, 2)))
                .thenReturn(new ReserveResponse(true, 8, "Зарезервировано"));

        orchestrationService.processCreating(request);

        verify(orderService).create(
                eq(request),
                argThat(snapshots -> hasPlaceholderSnapshot(snapshots, 10L, 2)),
                eq(OrderStatus.PENDING_CONFIRMATION),
                argThat(details -> details != null && details.contains("ручная проверка"))
        );
    }

    @Test
    void unavailableInventoryServiceSavesPendingOrderWithoutConfirmedReservation() {
        CreateOrderRequest request = createRequest(new OrderItemRequest(10L, 2));
        stubProduct(10L);
        when(inventoryClient.reserveStock(new ReserveRequest(10L, 2)))
                .thenThrow(new InventoryServiceUnavailableException(10L, new RuntimeException("Склад недоступен")));

        orchestrationService.processCreating(request);

        verify(orderService).create(
                eq(request),
                argThat(snapshots -> snapshots.size() == 1
                        && snapshots.get(0).productName().equals("Товар 10")
                        && snapshots.get(0).price().equals(new BigDecimal("125.50"))),
                eq(OrderStatus.PENDING_CONFIRMATION),
                argThat(details -> details != null && details.contains("Резервирование на складе не подтверждено"))
        );
    }

    private CreateOrderRequest createRequest(OrderItemRequest... items) {
        return new CreateOrderRequest("Иван", "ivan@example.com", List.of(items));
    }

    private void stubProduct(Long productId) {
        when(productClient.getProductById(productId)).thenReturn(new ProductDto(
                productId,
                "Товар " + productId,
                "Описание",
                new BigDecimal("125.50"),
                true
        ));
    }

    private boolean hasPlaceholderSnapshot(List<OrderItemSnapshot> snapshots, Long productId, Integer quantity) {
        return snapshots.size() == 1
                && snapshots.get(0).productId().equals(productId)
                && snapshots.get(0).productName().equals("Товар #" + productId + " (ожидает проверки)")
                && snapshots.get(0).price().equals(BigDecimal.ZERO)
                && snapshots.get(0).quantity().equals(quantity);
    }
}
