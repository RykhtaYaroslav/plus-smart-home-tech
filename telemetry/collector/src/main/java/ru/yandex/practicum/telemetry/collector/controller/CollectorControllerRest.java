package ru.yandex.practicum.telemetry.collector.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.yandex.practicum.telemetry.collector.model.hub.HubEvent;
import ru.yandex.practicum.telemetry.collector.model.sensor.SensorEvent;
import ru.yandex.practicum.telemetry.collector.service.CollectorService;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
public class CollectorControllerRest {
    private final CollectorService service;

    @PostMapping("/sensors")
    public void collectEvent(@RequestBody SensorEvent event) {
        service.collect(event);
    }

    @PostMapping("/hubs")
    public void collectEvent(@RequestBody HubEvent event) {
        service.collect(event);
    }
}
