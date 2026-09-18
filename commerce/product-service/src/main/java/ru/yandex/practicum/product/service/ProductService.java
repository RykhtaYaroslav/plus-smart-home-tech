package ru.yandex.practicum.product.service;

import ru.yandex.practicum.product.dto.CreateProductRequest;
import ru.yandex.practicum.product.dto.ProductDto;
import ru.yandex.practicum.product.dto.UpdateProductRequest;

import java.util.List;

public interface ProductService {
    List<ProductDto> getAllActiveProducts();

    ProductDto create(CreateProductRequest request);

    ProductDto findById(Long id);

    ProductDto update(Long id, UpdateProductRequest request);

    List<ProductDto> search(String query);

    List<ProductDto> findByCategoryId(Long categoryId);
}
