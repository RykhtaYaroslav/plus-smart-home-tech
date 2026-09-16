package ru.yandex.practicum.telemetry.analyzer.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.SensorsSnapshotAvro;
import ru.yandex.practicum.telemetry.analyzer.service.snapsot.SnapshotService;

import java.time.Duration;
import java.util.List;

@Component
@Slf4j
public class SnapshotProcessor implements Runnable {
    private final KafkaConsumer<String, SensorsSnapshotAvro> consumer;
    private final SnapshotService snapshotService;

    public SnapshotProcessor(@Qualifier("snapshotConsumer") KafkaConsumer<String, SensorsSnapshotAvro> consumer,
                             SnapshotService snapshotService) {
        this.consumer = consumer;
        this.snapshotService = snapshotService;
    }

    private static final Duration POLL_TIMEOUT = Duration.ofMillis(500);

    @Value("${analyzer.kafka.snapshot-consumer.topic}")
    private String topic;


    @Override
    public void run() {
        consumer.subscribe(List.of(topic));

        try {
            while (true) {
                ConsumerRecords<String, SensorsSnapshotAvro> records = consumer.poll(POLL_TIMEOUT);

                for (ConsumerRecord<String, SensorsSnapshotAvro> record : records) {
                    snapshotService.handle(record.value());
                }

                if (!records.isEmpty()) {
                    consumer.commitSync();
                }
            }
        } catch (WakeupException ignored) {
            // ignore
        } catch (Exception e) {
            log.error("Ошибка во время обработки снапшотов", e);
        } finally {
            try {
                consumer.commitSync();
            } finally {
                consumer.close();
            }
        }
    }
}
