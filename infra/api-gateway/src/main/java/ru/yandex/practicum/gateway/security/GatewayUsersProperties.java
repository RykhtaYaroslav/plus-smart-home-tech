package ru.yandex.practicum.gateway.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ConfigurationProperties(prefix = "app.security")
@Getter
@Setter
public class GatewayUsersProperties {

    private Map<String, User> users = new LinkedHashMap<>();

    @Getter
    @Setter
    public static class User {
        private String password;
        private List<String> roles = new ArrayList<>();
    }
}
