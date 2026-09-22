package ru.yandex.practicum.inventory.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.yandex.practicum.inventory.entity.InventoryItem;

public interface InventoryRepository extends JpaRepository<InventoryItem, Long> {
}
