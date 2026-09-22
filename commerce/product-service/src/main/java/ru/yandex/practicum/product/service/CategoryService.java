package ru.yandex.practicum.product.service;

import ru.yandex.practicum.product.dto.CategoryDto;
import ru.yandex.practicum.product.dto.CreateCategoryRequest;

import java.util.List;

public interface CategoryService {
    List<CategoryDto> getAll();

    CategoryDto create(CreateCategoryRequest request);

    CategoryDto findById(Long id);
}
