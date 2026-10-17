package ru.yandex.practicum.gateway.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.springframework.web.reactive.function.server.RequestPredicates.path;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@DisplayName("Правила безопасности Gateway")
class GatewaySecurityConfigTest {

    private static final String WEB_UI_ORIGIN = "http://localhost:8443";

    @Autowired
    private WebTestClient webTestClient;

    @Test
    @DisplayName("Каталог доступен без входа")
    void catalogGet_isPublic() {
        webTestClient.get().uri("/api/products")
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("Заказ без входа запрещён")
    void orderCreate_withoutCredentials_isUnauthorized() {
        webTestClient.post().uri("/api/orders")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @DisplayName("Пользователь может создать заказ")
    void orderCreate_withUserCredentials_passesSecurity() {
        webTestClient.post().uri("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, basic("ivan", "ivan-password"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("Пользователь не может изменить каталог")
    void productWrite_withUserCredentials_isForbidden() {
        webTestClient.post().uri("/api/products")
                .header(HttpHeaders.AUTHORIZATION, basic("ivan", "ivan-password"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("Администратор может изменить каталог")
    void productWrite_withAdminCredentials_passesSecurity() {
        webTestClient.post().uri("/api/products")
                .header(HttpHeaders.AUTHORIZATION, basic("anna", "anna-password"))
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    @DisplayName("Неизвестный маршрут закрыт")
    void unknownRoute_withAdminCredentials_isForbidden() {
        webTestClient.get().uri("/api/not-listed")
                .header(HttpHeaders.AUTHORIZATION, basic("anna", "anna-password"))
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    @DisplayName("CORS preflight разрешён")
    void corsPreflight_isAllowedAndContainsCorsHeaders() {
        webTestClient.method(HttpMethod.OPTIONS).uri("http://localhost/api/orders")
                .header(HttpHeaders.ORIGIN, WEB_UI_ORIGIN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, WEB_UI_ORIGIN);
    }

    private String basic(String username, String password) {
        String value = username + ":" + password;
        return "Basic " + Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    @TestConfiguration
    static class TestBackendConfig {

        @Bean
        RouteLocator testRouteLocator(RouteLocatorBuilder builder) {
            return builder.routes()
                    .route("test-backend", route -> route.path("/api/**")
                            .filters(filters -> filters.setPath("/test-backend"))
                            .uri("forward:/test-backend"))
                    .build();
        }

        @Bean
        RouterFunction<ServerResponse> testBackendRoutes() {
            return route(path("/test-backend"), request -> ServerResponse.ok().build());
        }
    }
}
