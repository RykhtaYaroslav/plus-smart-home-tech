package ru.yandex.practicum.order.service;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderDto;
import ru.yandex.practicum.order.dto.OrderItemRequest;
import ru.yandex.practicum.order.dto.OrderItemSnapshot;
import ru.yandex.practicum.order.dto.ProductDto;
import ru.yandex.practicum.order.dto.ReserveRequest;
import ru.yandex.practicum.order.exception.OrderProcessingException;
import ru.yandex.practicum.order.feign.InventoryClient;
import ru.yandex.practicum.order.feign.ProductClient;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
@Service
public class OrderOrchestrationServiceImpl implements OrderOrchestrationService {

    private final OrderService orderService;
    private final ProductClient productClient;
    private final InventoryClient inventoryClient;

    @Override
    public OrderDto processCreating(CreateOrderRequest request) {
        Map<Long, Integer> itemsAmount = new HashMap<>();
        List<OrderItemRequest> orderItems = request.items();
        orderItems.forEach(item -> itemsAmount.merge(item.productId(), item.quantity(), Integer::sum));

        Map<Long, ProductDto> products = findItems(itemsAmount.keySet());

        reserveItems(itemsAmount);

        List<OrderItemSnapshot> snapshots = orderItems.stream()
                .map(item -> {
                    ProductDto product = products.get(item.productId());
                    return new OrderItemSnapshot(product.id(), product.name(), product.price(), item.quantity());
                }).toList();
        return orderService.create(request, snapshots);
    }

    private Map<Long, ProductDto> findItems(Set<Long> ids) {
        Map<Long, ProductDto> products = new HashMap<>();

        try {
            for (Long productId : ids) {
                ProductDto product = productClient.getProductById(productId);
                if (!Boolean.TRUE.equals(product.active())) {
                    throw new OrderProcessingException("Товар с id = " + productId + " неактивен");
                }
                products.put(productId, product);
            }
            return products;
        } catch (FeignException.NotFound exception) {
            throw new OrderProcessingException("Товар не найден");
        } catch (FeignException exception) {
            throw new OrderProcessingException("Сервис товаров временно недоступен");
        }
    }

    private void reserveItems(Map<Long, Integer> itemsAmount) {
        try {
            itemsAmount.forEach((productId, quantity) ->
                    inventoryClient.reserveStock(new ReserveRequest(productId, quantity)));
        } catch (FeignException.NotFound exception) {
            throw new OrderProcessingException("Остатки товара не найдены");
        } catch (FeignException.ServiceUnavailable exception) {
            throw new OrderProcessingException("Сервис склада временно недоступен");
        } catch (FeignException exception) {
            throw new OrderProcessingException("Не удалось зарезервировать товар на складе");
        }
    }
}
