package ru.yandex.practicum.order;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import ru.yandex.practicum.order.dto.CreateOrderRequest;
import ru.yandex.practicum.order.dto.OrderItemRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@SuppressWarnings("unchecked")
class OrderServiceAcceptanceTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper json;

    @Test
    void shouldCreateOrderStoreProductSnapshotAndFindOrderByIdAndEmail() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                "Acceptance Buyer",
                "acceptance-buyer@example.com",
                List.of(
                        new OrderItemRequest(1L, "Acceptance Smart Lamp", 2, new BigDecimal("3490.00")),
                        new OrderItemRequest(2L, "Acceptance Smart Plug", 1, new BigDecimal("1290.00"))
                )
        );

        MvcResult createResponse = postJson("/api/orders", request);

        assertThat(status(createResponse))
                .as("POST /api/orders должен создавать заказ и возвращать HTTP 201 Created")
                .isEqualTo(201);
        Map<String, Object> created = readMap(createResponse);
        Long orderId = asLong(created.get("id"));
        assertThat(orderId)
                .as("Созданный заказ должен содержать поле id")
                .isNotNull();
        assertThat(created.get("status"))
                .as("На текущем этапе новый заказ должен сохраняться в статусе CREATED")
                .isEqualTo("CREATED");
        assertThat(asDecimal(created.get("totalPrice")))
                .as("order-service должен сам рассчитывать totalPrice по снимку товаров из запроса")
                .isEqualByComparingTo("8270.00");
        assertThat((List<?>) created.get("items"))
                .as("Заказ должен хранить позиции заказа")
                .hasSize(2)
                .anySatisfy(item -> assertThat((Map<String, Object>) item)
                        .as("Позиция заказа должна хранить снимок названия и цены товара из запроса")
                        .containsEntry("productName", "Acceptance Smart Lamp"));

        MvcResult byIdResponse = mvc.perform(get("/api/orders/{id}", orderId)).andReturn();

        assertThat(status(byIdResponse))
                .as("GET /api/orders/{id} должен возвращать созданный заказ")
                .isEqualTo(200);
        assertThat(readMap(byIdResponse).get("customerEmail"))
                .as("GET /api/orders/{id} должен вернуть заказ с ожидаемым email клиента")
                .isEqualTo("acceptance-buyer@example.com");
        Map<String, Object> stored = readMap(byIdResponse);
        assertThat(stored.get("createdAt")).isNotNull();
        assertThat(asDecimal(stored.get("totalPrice"))).isEqualByComparingTo("8270.00");
        assertThat((List<Map<String, Object>>) stored.get("items"))
                .hasSize(2)
                .allSatisfy(item -> assertThat(item.get("id")).isNotNull())
                .anySatisfy(item -> {
                    assertThat(item.get("productName")).isEqualTo("Acceptance Smart Lamp");
                    assertThat(asDecimal(item.get("price"))).isEqualByComparingTo("3490.00");
                    assertThat(item.get("quantity")).isEqualTo(2);
                });

        MvcResult byEmailResponse = mvc.perform(get("/api/orders/by-email")
                .param("email", "acceptance-buyer@example.com"))
                .andReturn();

        assertThat(status(byEmailResponse))
                .as("GET /api/orders/by-email?email=... должен возвращать заказы клиента")
                .isEqualTo(200);
        assertThat(readList(byEmailResponse))
                .as("Поиск заказов по email должен вернуть созданный заказ")
                .anySatisfy(item -> assertThat(item)
                        .containsEntry("customerEmail", "acceptance-buyer@example.com"));

        MvcResult allResponse = mvc.perform(get("/api/orders")).andReturn();
        assertThat(status(allResponse)).isEqualTo(200);
        assertThat(readList(allResponse))
                .anySatisfy(item -> assertThat(asLong(item.get("id"))).isEqualTo(orderId));

        MvcResult unknownEmailResponse = mvc.perform(get("/api/orders/by-email")
                .param("email", "unknown@example.com")).andReturn();
        assertThat(status(unknownEmailResponse)).isEqualTo(200);
        assertThat(readList(unknownEmailResponse)).isEmpty();
    }

    @Test
    void shouldReturnBadRequestForInvalidOrderPayload() throws Exception {
        CreateOrderRequest invalidRequest = new CreateOrderRequest(
                "",
                "not-an-email",
                List.of()
        );

        MvcResult response = postJson("/api/orders", invalidRequest);

        assertThat(status(response))
                .as("POST /api/orders с невалидным телом запроса должен возвращать HTTP 400 Bad Request")
                .isEqualTo(400);
        assertThat(readMap(response))
                .as("Ответ ошибки должен содержать сообщение и детали валидации")
                .containsKeys("message", "validationErrors");
    }

    @Test
    void shouldReturnNotFoundForUnknownOrder() throws Exception {
        MvcResult response = mvc.perform(get("/api/orders/{id}", Long.MAX_VALUE)).andReturn();

        assertThat(status(response)).isEqualTo(404);
        assertThat(readMap(response))
                .containsEntry("status", 404)
                .containsEntry("message", "Заказ с id = " + Long.MAX_VALUE + " не найден");
        assertThat(readMap(response).get("timestamp")).isNotNull();
    }

    @Test
    void shouldReturnBadRequestForNonPositiveOrderId() throws Exception {
        MvcResult response = mvc.perform(get("/api/orders/{id}", 0)).andReturn();

        assertThat(status(response)).isEqualTo(400);
        assertThat(readMap(response)).containsEntry("status", 400);
        assertThat(readMap(response).get("timestamp")).isNotNull();
    }

    @Test
    void shouldReturnValidationErrorsForInvalidOrderItem() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                "Buyer",
                "buyer@example.com",
                List.of(new OrderItemRequest(1L, "Lamp", 0, BigDecimal.ZERO))
        );

        MvcResult response = postJson("/api/orders", request);

        assertThat(status(response)).isEqualTo(400);
        assertThat((Map<String, String>) readMap(response).get("validationErrors"))
                .containsKeys("items[0].quantity", "items[0].price");
    }

    private MvcResult postJson(String url, Object body) throws Exception {
        return mvc.perform(post(url)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)))
                .andReturn();
    }

    private static int status(MvcResult result) {
        return result.getResponse().getStatus();
    }

    private Map<String, Object> readMap(MvcResult result) throws Exception {
        return json.readValue(result.getResponse().getContentAsByteArray(), new TypeReference<>() {
        });
    }

    private List<Map<String, Object>> readList(MvcResult result) throws Exception {
        return json.readValue(result.getResponse().getContentAsByteArray(), new TypeReference<>() {
        });
    }

    private static Long asLong(Object value) {
        return value == null ? null : ((Number) value).longValue();
    }

    private static BigDecimal asDecimal(Object value) {
        return new BigDecimal(value.toString());
    }
}
