package ru.yandex.practicum.telemetry.aggregator.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.yandex.practicum.kafka.telemetry.event.SensorEventAvro;
import ru.yandex.practicum.telemetry.mapper.SensorEventDeserializer;

import java.util.Properties;

@Configuration
public class KafkaConsumerConfig {
    @Value("${aggregator.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${aggregator.kafka.consumer.group-id}")
    private String groupId;

    @Value("${aggregator.kafka.consumer.client-id}")
    private String clientId;

    @Value("${aggregator.kafka.consumer.auto-offset-reset}")
    private String autoOffsetReset;

    @Value("${aggregator.kafka.consumer.max-poll-records}")
    private int maxPollRecords;

    @Value("${aggregator.kafka.consumer.max-poll-interval-ms}")
    private int maxPollIntervalMs;

    @Value("${aggregator.kafka.consumer.session-timeout-ms}")
    private int sessionTimeoutMs;

    @Value("${aggregator.kafka.consumer.heartbeat-interval-ms}")
    private int heartbeatIntervalMs;

    @Value("${aggregator.kafka.consumer.fetch-min-bytes}")
    private int fetchMinBytes;

    @Value("${aggregator.kafka.consumer.fetch-max-wait-ms}")
    private int fetchMaxWaitMs;

    @Bean
    public KafkaConsumer<String, SensorEventAvro> kafkaConsumer() {
        Properties properties = new Properties();

        // Адреса Kafka-брокеров, к которым consumer подключается при запуске.
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Идентификатор экземпляра consumer, который отображается в логах и метриках Kafka.
        properties.put(ConsumerConfig.CLIENT_ID_CONFIG, clientId);

        // Идентификатор consumer group: участники одной группы совместно обрабатывают партиции топика.
        properties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);

        // Десериализатор ключа Kafka-сообщения из массива байтов в String.
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);

        // Десериализатор значения; полное имя класса —
        // ru.yandex.practicum.telemetry.mapper.SensorEventDeserializer.
        properties.put(
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                SensorEventDeserializer.class
        );

        // Определяет начальную позицию чтения, если у группы ещё нет сохранённого offset.
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);

        // Отключает периодическое автоматическое подтверждение offset: подтверждать нужно после обработки данных.
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        // Ограничивает число записей, возвращаемых одним вызовом poll(), и размер пакета обработки.
        properties.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, maxPollRecords);

        // Максимальное время между вызовами poll(); превышение считается зависанием consumer.
        properties.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, maxPollIntervalMs);

        // Время, после которого брокер исключит consumer из группы при отсутствии heartbeat.
        properties.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, sessionTimeoutMs);

        // Интервал отправки heartbeat брокеру; должен быть заметно меньше session timeout.
        properties.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, heartbeatIntervalMs);

        // Минимальный объём данных для ответа на fetch-запрос; 1 байт минимизирует задержку получения событий.
        properties.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, fetchMinBytes);

        // Сколько брокер может ждать накопления fetch-min-bytes перед отправкой ответа consumer.
        properties.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, fetchMaxWaitMs);

        // Не позволяет незаметно создать топик из-за опечатки в его имени.
        properties.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, false);

        // Consumer видит только завершённые транзакции, если producer использует транзакционную запись.
        properties.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");

        return new KafkaConsumer<>(properties);
    }
}
