package ru.yandex.practicum.inventory.mapper;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.yandex.practicum.inventory.dto.InventoryDto;
import ru.yandex.practicum.inventory.dto.UpdateInventoryRequest;
import ru.yandex.practicum.inventory.entity.InventoryItem;

@Mapper(componentModel = "spring")
public interface InventoryServiceMapper {

    @Mapping(target = "id", source = "productId")
    InventoryDto toDto(InventoryItem item);

    @Mapping(target = "reservedQuantity", constant = "0")
    @Mapping(target = "version", ignore = true)
    InventoryItem toEntity(UpdateInventoryRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "productId", ignore = true)
    @Mapping(target = "reservedQuantity", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntity(UpdateInventoryRequest request, @MappingTarget InventoryItem item);
}
