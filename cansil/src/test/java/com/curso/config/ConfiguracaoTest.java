package com.curso.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import static org.assertj.core.api.Assertions.assertThat;

class ConfiguracaoTest {
    @Configuration @EnableConfigurationProperties({CotacaoProperties.class,JwtProperties.class})
    static class Config {}
    final ApplicationContextRunner runner=new ApplicationContextRunner().withUserConfiguration(Config.class)
        .withPropertyValues("cotacao.brapi.base-url=http://localhost","cotacao.brapi.token=fake",
            "cotacao.alphavantage.base-url=http://localhost","cotacao.alphavantage.token=fake",
            "cotacao.hgfinance.base-url=http://localhost","cotacao.hgfinance.token=fake",
            "jwt.secret=fake-secret-for-tests-only-123456789","jwt.expiration-seconds=3600");
    @Test void propriedadesValidasInicializam() { runner.run(context->assertThat(context).hasNotFailed()); }
    @Test void fontesDesativadasNaoExigemTokens() {
        runner.withPropertyValues("cotacao.alphavantage.enabled=false", "cotacao.alphavantage.token=",
            "cotacao.hgfinance.enabled=false", "cotacao.hgfinance.token=")
            .run(context -> assertThat(context).hasNotFailed());
    }
    @Test void reativarFonteSemTokenFalha() {
        runner.withPropertyValues("cotacao.alphavantage.enabled=true", "cotacao.alphavantage.token=")
            .run(context -> assertThat(context).hasFailed());
        runner.withPropertyValues("cotacao.hgfinance.enabled=true", "cotacao.hgfinance.token=")
            .run(context -> assertThat(context).hasFailed());
    }
    @Test void tokenVazioImpedeInicializacao() { runner.withPropertyValues("cotacao.brapi.token=").run(context->assertThat(context).hasFailed()); }
    @Test void naoPermiteDesativarFontePrimariaParaDispensarToken() {
        runner.withPropertyValues("cotacao.brapi.enabled=false", "cotacao.brapi.token=")
            .run(context -> assertThat(context).hasFailed());
    }
    @Test void segredoVazioImpedeInicializacao() { runner.withPropertyValues("jwt.secret=").run(context->assertThat(context).hasFailed()); }
    @Test void expiracaoInvalidaImpedeInicializacao() { runner.withPropertyValues("jwt.expiration-seconds=0").run(context->assertThat(context).hasFailed()); }
}
