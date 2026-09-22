package ru.yandex.practicum.order.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.yandex.practicum.order.dto.ReserveRequest;
import ru.yandex.practicum.order.dto.ReserveResponse;

@FeignClient(name = "inventory-service")
public interface InventoryClient {

    @PostMapping("/api/inventory/reserve")
    ReserveResponse reserveStock(@RequestBody ReserveRequest request);
}