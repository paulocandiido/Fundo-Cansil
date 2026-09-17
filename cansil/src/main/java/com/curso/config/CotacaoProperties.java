package com.curso.config;
import org.springframework.boot.context.properties.ConfigurationProperties;
@ConfigurationProperties(prefix="cotacao")
@org.springframework.validation.annotation.Validated
public record CotacaoProperties(@jakarta.validation.Valid @jakarta.validation.constraints.NotNull Fonte brapi,@jakarta.validation.Valid @jakarta.validation.constraints.NotNull Fonte alphavantage,@jakarta.validation.Valid @jakarta.validation.constraints.NotNull Fonte hgfinance) {
    @jakarta.validation.constraints.AssertTrue(message="A fonte primária brapi deve permanecer habilitada")
    public boolean isBrapiHabilitada() { return brapi != null && brapi.enabled(); }
    public record Fonte(@jakarta.validation.constraints.NotBlank String baseUrl, String token,
            @org.springframework.boot.context.properties.bind.DefaultValue("true") boolean enabled) {
        @org.springframework.boot.context.properties.bind.ConstructorBinding
        public Fonte {}
        public Fonte(String baseUrl, String token) { this(baseUrl, token, true); }

        @jakarta.validation.constraints.AssertTrue(message="Token obrigatório para fonte habilitada")
        public boolean isTokenConfigurado() { return !enabled || (token != null && !token.isBlank()); }
    }
}
