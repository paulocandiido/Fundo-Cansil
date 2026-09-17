package com.curso.cadastro;

import com.curso.domains.dtos.InvestimentoDTOs.CnpjResponse;
import com.curso.services.exceptions.CadastroIndisponivelException;
import com.curso.services.exceptions.ObjectNotFoundException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Clock;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.HttpClientErrorException;

@Component
public class BrasilApiCnpjClient {
    private final RestClient client;
    private final Clock clock;
    public BrasilApiCnpjClient(RestClient.Builder builder, Clock clock,
            @Value("${cadastro.brasilapi.base-url:https://brasilapi.com.br}") String baseUrl) {
        this.client = builder.clone().baseUrl(baseUrl).build();
        this.clock = clock;
    }
    public CnpjResponse consultar(String entrada) {
        String cnpj = Cnpj.normalizar(entrada);
        try {
            Empresa e = client.get().uri("/api/cnpj/v1/{cnpj}", cnpj).retrieve().body(Empresa.class);
            if (e == null || !cnpj.equals(Cnpj.normalizar(e.cnpj())) || e.razaoSocial() == null
                    || e.razaoSocial().isBlank() || e.razaoSocial().length() > 254
                    || e.razaoSocial().chars().anyMatch(Character::isISOControl)
                    || e.situacao() == null || !Set.of(1, 2, 3, 4, 8).contains(e.situacao()))
                throw new CadastroIndisponivelException();
            String situacao = switch (e.situacao()) {
                case 1 -> "NULA"; case 2 -> "ATIVA"; case 3 -> "SUSPENSA"; case 4 -> "INAPTA"; default -> "BAIXADA";
            };
            return new CnpjResponse(cnpj, e.razaoSocial().trim(), situacao, e.situacao() == 2, clock.instant());
        } catch (HttpClientErrorException.NotFound ex) {
            throw new ObjectNotFoundException("CNPJ não encontrado na fonte cadastral");
        } catch (RestClientException | IllegalArgumentException ex) {
            throw new CadastroIndisponivelException();
        }
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Empresa(String cnpj, @JsonProperty("razao_social") String razaoSocial,
            @JsonProperty("situacao_cadastral") Integer situacao) {}
}
