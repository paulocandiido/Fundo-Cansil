package com.curso.cotacao;

import com.curso.config.CotacaoProperties;
import com.curso.domains.enums.FonteCotacao;
import com.fasterxml.jackson.annotation.*;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.*;
import java.math.BigDecimal;
import java.util.*;
import static com.curso.cotacao.ProviderCotacaoException.Categoria.*;

@Order(30)
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(prefix="cotacao.hgfinance", name="enabled", havingValue="true", matchIfMissing=true)
public class HgFinanceProvider implements CotacaoProvider {
    private final RestClient client;
    private final String token;
    public HgFinanceProvider(RestClient.Builder builder,CotacaoProperties properties) {
        client=builder.baseUrl(properties.hgfinance().baseUrl()).build();
        token=properties.hgfinance().token();
    }
    public FonteCotacao fonte() { return FonteCotacao.HG_FINANCE; }

    public CotacaoAtual buscar(String valor) {
        String simbolo=Simbolos.normalizar(valor);
        try {
            Resposta resposta=client.get().uri(u->u.path("/finance/stock_price")
                .queryParam("key",token).queryParam("symbol",simbolo).build())
                .retrieve().body(Resposta.class);
            if (resposta!=null&&Boolean.FALSE.equals(resposta.validKey()))
                throw new ProviderCotacaoException(fonte(),AUTENTICACAO);
            Resultados resultados=resposta==null?null:resposta.results();
            if (resultados!=null&&Boolean.TRUE.equals(resultados.error)) {
                String texto=Objects.toString(resultados.message,"").toLowerCase(Locale.ROOT);
                if (texto.contains("plano")||texto.contains("plan")||texto.contains("premium"))
                    throw new ProviderCotacaoException(fonte(),PLANO);
                if (texto.contains("limit")||texto.contains("quota"))
                    throw new ProviderCotacaoException(fonte(),LIMITE);
                throw new ProviderCotacaoException(fonte(),ERRO_DO_PROVEDOR);
            }
            Acao acao=resultados==null?null:resultados.acoes.stream()
                .filter(a->simbolo.equalsIgnoreCase(a.symbol())).findFirst().orElse(null);
            if (acao==null||acao.price()==null||acao.price().signum()<=0)
                throw new ProviderCotacaoException(fonte(),COTACAO_AUSENTE);
            return new CotacaoAtual(simbolo,acao.name()==null?simbolo:acao.name(),"B3",acao.price(),fonte());
        } catch (ProviderCotacaoException ex) { throw ex; }
        catch (RestClientException ex) { throw new ProviderCotacaoException(fonte(),"Falha ao consultar HG Finance",ex); }
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    public record Resposta(@JsonProperty("valid_key") Boolean validKey,Resultados results) {}
    public static class Resultados {
        @JsonProperty("error") private Boolean error;
        @JsonProperty("message") private String message;
        private final List<Acao> acoes=new ArrayList<>();
        @JsonAnySetter public void adicionar(String simbolo,Acao acao) {
            if (acao!=null) acoes.add(acao);
        }
    }
    @JsonIgnoreProperties(ignoreUnknown=true)
    public record Acao(String symbol,String name,BigDecimal price) {}
}
