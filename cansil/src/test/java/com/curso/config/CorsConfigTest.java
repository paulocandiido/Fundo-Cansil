package com.curso.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CorsConfigTest {
    @Test void rejeitaCuringaECaminho() {
        var config = new CorsConfig();
        for (String origin : new String[]{"*", "https://*.example.com", "http://localhost:5173/", "https://example.com/front", "null", "https://user@example.com"}) {
            assertThrows(IllegalArgumentException.class, () -> config.corsConfigurationSource(origin));
        }
    }

    @Test void aceitaListaExplicitaEVazia() {
        var config = new CorsConfig();
        assertNotNull(config.corsConfigurationSource("https://front.example.com, http://localhost:5173"));
        assertNotNull(config.corsConfigurationSource(""));
    }
}
