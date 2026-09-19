package ru.yandex.practicum.product.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.product.dto.CreateProductRequest;
import ru.yandex.practicum.product.dto.ProductDto;
import ru.yandex.practicum.product.dto.UpdateProductRequest;
import ru.yandex.practicum.product.entity.Category;
import ru.yandex.practicum.product.entity.Product;
import ru.yandex.practicum.product.exception.NotFoundException;
import ru.yandex.practicum.product.mapper.ProductServiceMapper;
import ru.yandex.practicum.product.repository.CategoryRepository;
import ru.yandex.practicum.product.repository.ProductRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductServiceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> getAllActiveProducts() {
        List<Product> products = productRepository.findAllByActive(true);

        return products.stream().map(mapper::toDto).toList();
    }

    @Override
    public ProductDto create(CreateProductRequest request) {
        Category category = request.categoryId() == null ? null : getCategoryOrThrow(request.categoryId());

        Product product = mapper.toEntity(request, category);

        productRepository.save(product);

        return mapper.toDto(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDto findById(Long id) {
        Product product = getProductOrThrow(id);
        return mapper.toDto(product);
    }

    @Override
    public ProductDto update(Long id, UpdateProductRequest request) {
        Product product = getProductOrThrow(id);
        Category category = null;

        if (request.categoryId() != null) {
            category = getCategoryOrThrow(request.categoryId());
        }

        mapper.updateEntity(request, category, product);

        productRepository.save(product);

        return mapper.toDto(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> search(String query) {
        List<Product> products = productRepository.findByNameContainingIgnoreCase(query);
        return products.stream().map(mapper::toDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductDto> findByCategoryId(Long categoryId) {
        getCategoryOrThrow(categoryId);

        List<Product> products = productRepository.findAllByCategoryId(categoryId);

        return products.stream().map(mapper::toDto).toList();
    }

    private Category getCategoryOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> {
                    String m = String.format("Категория с id = %d не найдена", id);
                    return new NotFoundException(m);
                });
    }

    private Product getProductOrThrow(Long id) {
        return productRepository.findById(id).orElseThrow(() -> {
            String m = String.format("Продукт с id = %d не найдена", id);
            return new NotFoundException(m);
        });
    }
}
