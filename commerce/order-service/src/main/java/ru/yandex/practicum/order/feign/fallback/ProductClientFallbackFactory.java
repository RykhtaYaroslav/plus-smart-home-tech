package ru.yandex.practicum.order.feign.fallback;

import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.order.exception.ProductServiceUnavailableException;
import ru.yandex.practicum.order.feign.ProductClient;

@Component
@Slf4j
public class ProductClientFallbackFactory implements FallbackFactory<ProductClient> {

    @Override
    public ProductClient create(Throwable cause) {
        return productId -> {
            FeignException feignException = findFeignException(cause);
            if (feignException != null && feignException.status() >= 400 && feignException.status() < 500) {
                throw feignException;
            }

            log.warn("product-service недоступен при запросе товара id={}", productId, cause);

            throw new ProductServiceUnavailableException(productId, cause);
        };
    }

    /** Ищет FeignException в цепочке причин, чтобы сохранить исходный HTTP-статус ответа. */
    private FeignException findFeignException(Throwable cause) {
        for (Throwable current = cause; current != null; current = current.getCause()) {
            if (current instanceof FeignException feignException) {
                return feignException;
            }
        }
        return null;
    }
}
