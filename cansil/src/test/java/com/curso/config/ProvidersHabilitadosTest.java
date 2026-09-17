package com.curso.config;

import com.curso.cotacao.*;
import com.curso.domains.enums.FonteCotacao;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestClient;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.http.HttpStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;

class ProvidersHabilitadosTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withUserConfiguration(CotacaoConfig.class, BrapiProvider.class, AlphaVantageProvider.class,
            HgFinanceProvider.class, CotacaoOrchestrator.class)
        .withPropertyValues("cotacao.brapi.base-url=http://localhost", "cotacao.brapi.token=fake",
            "cotacao.alphavantage.base-url=http://localhost", "cotacao.alphavantage.token=",
            "cotacao.hgfinance.base-url=http://localhost", "cotacao.hgfinance.token=",
            "cotacao.alphavantage.enabled=false", "cotacao.hgfinance.enabled=false");

    @Test void somenteBrapiRegistradaSemTokensDeFallback() {
        runner.withBean(RestClient.Builder.class, RestClient::builder).run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(BrapiProvider.class)
                .doesNotHaveBean(AlphaVantageProvider.class).doesNotHaveBean(HgFinanceProvider.class);
            assertThat(context.getBeansOfType(CotacaoProvider.class)).hasSize(1);
        });
    }

    @Test void reativacaoRestauraOsTresProviders() {
        runner.withBean(RestClient.Builder.class, RestClient::builder).withPropertyValues("cotacao.alphavantage.enabled=true", "cotacao.alphavantage.token=fake",
            "cotacao.hgfinance.enabled=true", "cotacao.hgfinance.token=fake")
            .run(context -> {
                assertThat(context).hasNotFailed().hasSingleBean(AlphaVantageProvider.class).hasSingleBean(HgFinanceProvider.class);
                assertThat(context.getBeansOfType(CotacaoProvider.class)).hasSize(3);
            });
    }

    @Test void falhaBrapiNaoTentaFontesDesativadas() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("http://localhost/api/v2/stocks/quote?symbols=PETR4"))
            .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        runner.withBean(RestClient.Builder.class, () -> builder).run(context -> {
            assertThat(context).hasNotFailed();
            var failure = assertThrows(CotacaoIndisponivelException.class,
                () -> context.getBean(CotacaoOrchestrator.class).buscar("PETR4"));
            assertThat(failure.getMessage()).contains(FonteCotacao.BRAPI.name())
                .doesNotContain("ALPHA_VANTAGE", "HG_FINANCE");
            server.verify();
        });
    }
}
