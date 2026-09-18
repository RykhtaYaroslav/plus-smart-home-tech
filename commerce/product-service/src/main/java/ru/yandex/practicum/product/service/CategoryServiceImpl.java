package ru.yandex.practicum.product.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.product.dto.CategoryDto;
import ru.yandex.practicum.product.dto.CreateCategoryRequest;
import ru.yandex.practicum.product.entity.Category;
import ru.yandex.practicum.product.exception.NotFoundException;
import ru.yandex.practicum.product.mapper.ProductServiceMapper;
import ru.yandex.practicum.product.repository.CategoryRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final ProductServiceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<CategoryDto> getAll() {
        return categoryRepository.findAll().stream().map(mapper::toDto).toList();
    }

    @Override
    public CategoryDto create(CreateCategoryRequest request) {
        Category category = categoryRepository.save(mapper.toEntity(request));
        return mapper.toDto(category);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryDto findById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(
                        String.format("Категория с id = %d не найдена", id)));
        return mapper.toDto(category);
    }
}
