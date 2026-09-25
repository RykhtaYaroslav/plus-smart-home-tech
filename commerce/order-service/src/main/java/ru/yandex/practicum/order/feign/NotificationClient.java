package ru.yandex.practicum.order.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

// DTO

record NotificationRequest(
        Long orderId,
        String email,
        String message
) {
}

record NotificationResponse(
        Long id,
        Long orderId,
        String status
) {
}

// Feign-клиент

@FeignClient(name = "notification-service")
public interface NotificationClient {

    @PostMapping("/api/notifications")
    void send(@RequestBody NotificationRequest request);

    @GetMapping("/api/notifications/order/{orderId} ")
    List<NotificationResponse> findByOrderId(@PathVariable("orderId") Long orderId);
}