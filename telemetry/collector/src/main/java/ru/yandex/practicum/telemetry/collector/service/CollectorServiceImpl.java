package ru.yandex.practicum.telemetry.collector.service;

import org.springframework.stereotype.Service;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.telemetry.collector.kafka.EventKafkaProducerImpl;
import ru.yandex.practicum.telemetry.collector.model.hub.HubEvent;
import ru.yandex.practicum.telemetry.collector.model.mapper.HubEventMapper;
import ru.yandex.practicum.telemetry.collector.model.mapper.SensorEventMapper;
import ru.yandex.practicum.telemetry.collector.model.sensor.SensorEvent;
import ru.yandex.practicum.telemetry.collector.model.sensor.SensorEventType;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CollectorServiceImpl implements CollectorService {
    private final HubEventMapper hubMapper;
    private final Map<SensorEventType, SensorEventMapper> sensorEventMapperMap;
    private final EventKafkaProducerImpl kafka;

    public CollectorServiceImpl(HubEventMapper hubMapper,
                                List<SensorEventMapper> sensorEventMappers,
                                EventKafkaProducerImpl kafka) {
        this.hubMapper = hubMapper;
        this.sensorEventMapperMap = sensorEventMappers.stream()
                .collect(Collectors.toUnmodifiableMap(
                        SensorEventMapper::getMessageType,
                        Function.identity()
                ));
        this.kafka = kafka;
    }

    @Override
    public void collect(SensorEvent event) {
        SensorEventMapper mapper = sensorEventMapperMap.get(event.getType());

        if (mapper == null) {
            throw new IllegalStateException("Sensor event mapper not found for type: " + event.getType());
        }

        SensorEventAvro avro = mapper.mapToAvro(event);
        kafka.send(avro);
    }

    @Override
    public void collect(HubEvent event) {
        HubEventAvro avro = hubMapper.map(event);
        kafka.send(avro);
    }
}
