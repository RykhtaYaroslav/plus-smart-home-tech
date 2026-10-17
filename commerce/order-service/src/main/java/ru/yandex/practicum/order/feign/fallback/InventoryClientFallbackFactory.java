package ru.yandex.practicum.order.feign.fallback;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.order.dto.ReserveRequest;
import ru.yandex.practicum.order.dto.ReserveResponse;
import ru.yandex.practicum.order.exception.InventoryServiceUnavailableException;
import ru.yandex.practicum.order.feign.InventoryClient;

@Component
@Slf4j
public class InventoryClientFallbackFactory implements FallbackFactory<InventoryClient> {

    @Override
    public InventoryClient create(Throwable cause) {
        return new InventoryClient() {

            @Override
            public ReserveResponse reserveStock(ReserveRequest request) {
                FeignException feignException = findFeignException(cause);
                if (isBusinessResponse(feignException)) {
                    throw feignException;
                }
                log.warn(
                        "inventory-service недоступен при резервировании товара id={}",
                        request.productId(),
                        cause
                );

                throw new InventoryServiceUnavailableException(request.productId(), cause);
            }

            @Override
            public ReserveResponse releaseStock(ReserveRequest request) {
                FeignException feignException = findFeignException(cause);
                if (isBusinessResponse(feignException)) {
                    throw feignException;
                }
                log.warn(
                        "inventory-service недоступен при снятии резерва товара id={}",
                        request.productId(),
                        cause
                );

                throw new InventoryServiceUnavailableException(request.productId(), cause);
            }
        };
    }

    /** Ищет FeignException в цепочке причин, чтобы отличить HTTP-ответ от технической ошибки. */
    private FeignException findFeignException(Throwable cause) {
        for (Throwable current = cause; current != null; current = current.getCause()) {
            if (current instanceof FeignException feignException) {
                return feignException;
            }
        }
        return null;
    }

    /** Проверяет, что Feign получил бизнес-ответ с клиентским статусом 4xx. */
    private boolean isBusinessResponse(FeignException feignException) {
        return feignException != null && feignException.status() >= 400 && feignException.status() < 500;
    }
}
