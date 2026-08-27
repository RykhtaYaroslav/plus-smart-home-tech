package ru.yandex.practicum.telemetry.collector.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.telemetry.collector.kafka.EventKafkaProducerImpl;
import ru.yandex.practicum.telemetry.collector.model.hub.HubEvent;
import ru.yandex.practicum.telemetry.collector.model.mapper.HubEventMapper;
import ru.yandex.practicum.telemetry.collector.model.mapper.SensorEventMapper;
import ru.yandex.practicum.telemetry.collector.model.sensor.SensorEvent;

@Service
@RequiredArgsConstructor
public class CollectorServiceImpl implements CollectorService {
    private final HubEventMapper hubMapper;
    private final SensorEventMapper sensorMapper;
    private final EventKafkaProducerImpl kafka;

    @Override
    public void collect(SensorEvent event) {
        SensorEventAvro avro = sensorMapper.map(event);
        kafka.send(avro);
    }

    @Override
    public void collect(HubEvent event) {
        HubEventAvro avro = hubMapper.map(event);
        kafka.send(avro);
    }
}
