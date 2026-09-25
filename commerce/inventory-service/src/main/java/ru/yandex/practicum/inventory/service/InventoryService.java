package ru.yandex.practicum.inventory.service;

import ru.yandex.practicum.inventory.dto.InventoryDto;
import ru.yandex.practicum.inventory.dto.ReserveRequest;
import ru.yandex.practicum.inventory.dto.ReserveResponse;
import ru.yandex.practicum.inventory.dto.UpdateInventoryRequest;

import java.util.List;

public interface InventoryService {
    List<InventoryDto> getAll();

    InventoryDto update(UpdateInventoryRequest request);

    InventoryDto create(UpdateInventoryRequest request);

    ReserveResponse reserve(ReserveRequest request);

    ReserveResponse release(ReserveRequest request);

    InventoryDto findByProductId(Long productId);
}
