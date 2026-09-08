package ru.yandex.practicum.telemetry.analyzer.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.WakeupException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.kafka.telemetry.event.HubEventAvro;
import ru.yandex.practicum.telemetry.analyzer.service.HubEventService;

import java.time.Duration;
import java.util.List;

@RequiredArgsConstructor
@Component
@Slf4j
public class HubEventProcessor implements Runnable {
    private final KafkaConsumer<String, HubEventAvro> consumer;
    private final HubEventService hubEventService;

    private static final Duration POLL_TIMEOUT = Duration.ofMillis(500);

    @Value("${analyzer.kafka.hub-event-consumer.topic}")
    private String topic;

    @Override
    public void run() {
        consumer.subscribe(List.of(topic));

        try {
            while (true) {
                ConsumerRecords<String, HubEventAvro> records = consumer.poll(POLL_TIMEOUT);

                for (ConsumerRecord<String, HubEventAvro> record : records) {
                    hubEventService.handle(record.value());
                }

                if (!records.isEmpty()) {
                    consumer.commitSync();
                }
            }
        } catch (WakeupException ignored) {
            // ignore
        } catch (Exception e) {
            log.error("Ошибка во время обработки событий хаба", e);
        } finally {
            try {
                consumer.commitSync();
            } finally {
                consumer.close();
            }
        }
    }
}
