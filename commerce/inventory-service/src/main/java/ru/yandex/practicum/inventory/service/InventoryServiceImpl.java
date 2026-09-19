package ru.yandex.practicum.inventory.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.yandex.practicum.inventory.dto.InventoryDto;
import ru.yandex.practicum.inventory.dto.ReserveRequest;
import ru.yandex.practicum.inventory.dto.ReserveResponse;
import ru.yandex.practicum.inventory.dto.UpdateInventoryRequest;
import ru.yandex.practicum.inventory.entity.InventoryItem;
import ru.yandex.practicum.inventory.exception.ConflictException;
import ru.yandex.practicum.inventory.exception.InsufficientStockException;
import ru.yandex.practicum.inventory.exception.NotFoundException;
import ru.yandex.practicum.inventory.mapper.InventoryServiceMapper;
import ru.yandex.practicum.inventory.repository.InventoryRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryServiceImpl implements InventoryService {
    private final InventoryRepository inventoryRepository;
    private final InventoryServiceMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public List<InventoryDto> getAll() {
        return inventoryRepository.findAll().stream()
                .map(mapper::toDto)
                .toList();
    }

    @Override
    public InventoryDto update(UpdateInventoryRequest request) {
        InventoryItem item = getInventoryOrThrow(request.productId());
        ensureQuantityCanBeUpdated(item, request.quantity());

        mapper.updateEntity(request, item);
        inventoryRepository.save(item);

        return mapper.toDto(item);
    }

    @Override
    public InventoryDto create(UpdateInventoryRequest request) {
        ensureInventoryDoesNotExist(request.productId());

        InventoryItem item = mapper.toEntity(request);
        inventoryRepository.save(item);

        return mapper.toDto(item);
    }

    @Override
    public ReserveResponse reserve(ReserveRequest request) {
        InventoryItem item = getInventoryOrThrow(request.productId());
        ensureStockIsAvailable(item, request.quantity());

        item.setReservedQuantity(item.getReservedQuantity() + request.quantity());
        inventoryRepository.save(item);

        String message = String.format("Товар с productId = %d успешно зарезервирован", request.productId());
        return new ReserveResponse(true, item.getAvailableQuantity(), message);
    }

    @Override
    @Transactional(readOnly = true)
    public InventoryDto findByProductId(Long productId) {
        return mapper.toDto(getInventoryOrThrow(productId));
    }

    private InventoryItem getInventoryOrThrow(Long productId) {
        return inventoryRepository.findById(productId)
                .orElseThrow(() -> {
                    String message = String.format("Остатки для товара с productId = %d не найдены", productId);
                    return new NotFoundException(message);
                });
    }

    private void ensureInventoryDoesNotExist(Long productId) {
        if (inventoryRepository.existsById(productId)) {
            String message = String.format("Остатки для товара с productId = %d уже существуют", productId);
            throw new ConflictException(message);
        }
    }

    private void ensureQuantityCanBeUpdated(InventoryItem item, Integer quantity) {
        if (quantity < item.getReservedQuantity()) {
            String message = String.format(
                    "Количество товара с productId = %d не может быть меньше зарезервированного количества %d",
                    item.getProductId(), item.getReservedQuantity());
            throw new ConflictException(message);
        }
    }

    private void ensureStockIsAvailable(InventoryItem item, Integer requestedQuantity) {
        if (item.getAvailableQuantity() < requestedQuantity) {
            String message = String.format(
                    "Недостаточно товара с productId = %d: доступно %d, запрошено %d",
                    item.getProductId(), item.getAvailableQuantity(), requestedQuantity);
            throw new InsufficientStockException(message);
        }
    }
}
