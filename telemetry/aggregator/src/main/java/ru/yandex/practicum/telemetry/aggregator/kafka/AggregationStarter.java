package ru.yandex.practicum.telemetry.aggregator.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.telemetry.aggregator.AggregationService;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Future;

@Component
@RequiredArgsConstructor
@Slf4j
public class AggregationStarter {
    private final KafkaConsumer<String, SensorEventAvro> consumer;
    private final KafkaProducer<String, SensorsSnapshotAvro> producer;

    private final AggregationService aggregationService;

    private static final Duration POLL_TIMEOUT = Duration.ofMillis(500);

    @Value("${aggregator.kafka.sensor-topic}")
    private String sensorTopic;

    @Value("${aggregator.kafka.snapshot-topic}")
    private String snapshotTopic;

    public void start() {
        consumer.subscribe(List.of(sensorTopic));

        Runtime.getRuntime().addShutdownHook(new Thread(consumer::wakeup));

        try {
            while (true) {
                ConsumerRecords<String, SensorEventAvro> records = consumer.poll(POLL_TIMEOUT);

                List<Future<RecordMetadata>> pendingSends = new ArrayList<>();

                for (ConsumerRecord<String, SensorEventAvro> record : records) {
                    SensorEventAvro event = record.value();

                    Optional<SensorsSnapshotAvro> snapshot = aggregationService.updateState(event);

                    if (snapshot.isPresent()) {
                        SensorsSnapshotAvro value = snapshot.get();

                        ProducerRecord<String, SensorsSnapshotAvro> message = new ProducerRecord<>(
                                snapshotTopic,
                                value.getHubId(),
                                value
                        );

                        // Отправка остаётся асинхронной, а её результат проверяется
                        // перед подтверждением offsets текущего poll().
                        pendingSends.add(producer.send(message));
                    }
                }

                if (!records.isEmpty()) {
                    producer.flush();

                    // flush() дожидается завершения буферизованных отправок,
                    // но Future нужен для явной проверки ошибок доставки.
                    for (Future<RecordMetadata> pendingSend : pendingSends) {
                        pendingSend.get();
                    }

                    // Подтверждаем пакет только после успешной обработки всех
                    // его записей и подтверждения всех отправок снапшотов.
                    consumer.commitSync();
                }
            }

        } catch (WakeupException ignored) {
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий", e);
        } finally {
            try {
                producer.flush();
            } finally {
                consumer.close();
                producer.close();
            }
        }
    }
}
