package ru.yandex.practicum.product.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.product.dto.CreateProductRequest;
import ru.yandex.practicum.product.dto.ProductDto;
import ru.yandex.practicum.product.dto.UpdateProductRequest;
import ru.yandex.practicum.product.service.ProductService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
@Validated
public class ProductController {
    private final ProductService service;

    @GetMapping
    public List<ProductDto> getAllActiveProducts() {
        return service.getAllActiveProducts();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDto create(@Valid @RequestBody CreateProductRequest request) {
        return service.create(request);
    }

    @GetMapping("/{id}")
    public ProductDto findById(@PathVariable @Positive Long id) {
        return service.findById(id);
    }

    @PatchMapping("/{id}")
    public ProductDto update(@PathVariable @Positive Long id,
                             @Valid @RequestBody UpdateProductRequest request) {
        return service.update(id, request);
    }

    @GetMapping("/search")
    public List<ProductDto> search(@RequestParam String query) {
        return service.search(query);
    }

    @GetMapping("/category/{categoryId}")
    public List<ProductDto> findByCategoryId(@PathVariable @Positive Long categoryId) {
        return service.findByCategoryId(categoryId);
    }
}
