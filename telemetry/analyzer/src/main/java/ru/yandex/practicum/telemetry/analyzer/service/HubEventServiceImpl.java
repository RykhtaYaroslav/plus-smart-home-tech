package ru.yandex.practicum.telemetry.analyzer.service;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class HubEventServiceImpl implements HubEventService {
    private final Map<Class<?>, HubEventHandler> handlers;

    public HubEventServiceImpl(Set<HubEventHandler> handlers) {
        this.handlers = handlers.stream().collect(Collectors.toUnmodifiableMap(
                HubEventHandler::getPayloadType,
                Function.identity()
        ));
    }

    @Override
    public void handle(HubEventAvro hubEventAvro) {
        Class<?> clazz = hubEventAvro.getPayload().getClass();

        HubEventHandler handler = handlers.get(clazz);

        if (handler == null) {
            String m = "Неверный тип события: " + clazz.getName();
            throw new IllegalArgumentException(m);
        }
        handler.handle(hubEventAvro);
    }
}
