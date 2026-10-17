package ru.yandex.practicum.gateway.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.cloud.gateway.config.GlobalCorsProperties;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;

import java.util.List;

@Configuration
@EnableWebFluxSecurity
@EnableConfigurationProperties(GatewayUsersProperties.class)
public class GatewaySecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(
            ServerHttpSecurity http,
            CorsConfigurationSource corsConfigurationSource
    ) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
                .httpBasic(Customizer.withDefaults())
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .logout(ServerHttpSecurity.LogoutSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .pathMatchers(HttpMethod.GET,
                                "/api/products", "/api/products/**",
                                "/api/categories", "/api/categories/**",
                                "/api/inventory", "/api/inventory/**").permitAll()
                        .pathMatchers("/api/orders", "/api/orders/**").hasAnyRole("USER", "ADMIN")
                        .pathMatchers("/api/products", "/api/products/**",
                                "/api/categories", "/api/categories/**",
                                "/api/inventory", "/api/inventory/**").hasRole("ADMIN")
                        .anyExchange().denyAll())
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(GlobalCorsProperties gatewayCorsProperties) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        gatewayCorsProperties.getCorsConfigurations()
                .forEach(source::registerCorsConfiguration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public MapReactiveUserDetailsService userDetailsService(
            GatewayUsersProperties properties,
            PasswordEncoder passwordEncoder
    ) {
        List<UserDetails> users = properties.getUsers().entrySet().stream()
                .map(entry -> {
                    GatewayUsersProperties.User configuredUser = entry.getValue();
                    return User.withUsername(entry.getKey())
                            .password(passwordEncoder.encode(configuredUser.getPassword()))
                            .roles(configuredUser.getRoles().toArray(String[]::new))
                            .build();
                })
                .map(user -> (UserDetails) user)
                .toList();
        return new MapReactiveUserDetailsService(users);
    }
}
