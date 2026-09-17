package com.curso.config;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {
    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:4200,http://localhost:5173,http://127.0.0.1:3000,http://127.0.0.1:4200,http://127.0.0.1:5173}") String configuredOrigins) {
        var origins = Arrays.stream(configuredOrigins.split(",")).map(String::trim)
            .filter(value -> !value.isEmpty()).distinct().toList();
        for (String origin : origins) {
            URI uri = URI.create(origin);
            if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                    || uri.getHost() == null || uri.getRawUserInfo() != null
                    || !uri.getRawPath().isEmpty() || uri.getRawQuery() != null
                    || uri.getRawFragment() != null || origin.contains("*")) {
                throw new IllegalArgumentException("CORS exige origens HTTP(S) explícitas, sem caminho ou curinga");
            }
        }
        var cors = new CorsConfiguration();
        cors.setAllowedOrigins(origins);
        cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        cors.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        cors.setAllowCredentials(false);
        cors.setMaxAge(3600L);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }
}
