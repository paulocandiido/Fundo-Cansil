package com.curso.cotacao;

import com.curso.config.CotacaoProperties;
import com.curso.domains.enums.FonteCotacao;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import static com.curso.cotacao.ProviderCotacaoException.Categoria.*;

@Order(20)
@Component
@org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(prefix="cotacao.alphavantage", name="enabled", havingValue="true", matchIfMissing=true)
public class AlphaVantageProvider implements CotacaoProvider {
    private final RestClient client;
    private final String token;

    public AlphaVantageProvider(RestClient.Builder builder, CotacaoProperties properties) {
        client=builder.baseUrl(properties.alphavantage().baseUrl()).build();
        token=properties.alphavantage().token();
    }

    public FonteCotacao fonte() { return FonteCotacao.ALPHA_VANTAGE; }

    public CotacaoAtual buscar(String valor) {
        String simbolo=Simbolos.normalizar(valor);
        try {
            Busca busca=client.get().uri(u->u.path("/query")
                .queryParam("function","SYMBOL_SEARCH").queryParam("keywords",simbolo)
                .queryParam("apikey",token).build()).retrieve().body(Busca.class);
            if (busca!=null) validarErro(busca.information(),busca.note(),busca.error());
            Correspondencia ativo=busca==null||busca.matches()==null?null:busca.matches().stream()
                .filter(Objects::nonNull)
                .filter(m->m.symbol()!=null&&m.symbol().split("\\.")[0].equalsIgnoreCase(simbolo)
                    &&"BRL".equalsIgnoreCase(m.currency()))
                .findFirst().orElse(null);
            if (ativo==null) throw new ProviderCotacaoException(fonte(),BUSCA_SEM_ATIVO);
            Resposta resposta=client.get().uri(u->u.path("/query")
                .queryParam("function","GLOBAL_QUOTE").queryParam("symbol",ativo.symbol())
                .queryParam("apikey",token).build()).retrieve().body(Resposta.class);
            if (resposta!=null) validarErro(resposta.information(),resposta.note(),resposta.error());
            String price=resposta==null||resposta.quote()==null?null:resposta.quote().price();
            if (price==null||price.isBlank()) throw new ProviderCotacaoException(fonte(),COTACAO_AUSENTE);
            BigDecimal preco=new BigDecimal(price);
            if (preco.signum()<=0) throw new ProviderCotacaoException(fonte(),COTACAO_AUSENTE);
            return new CotacaoAtual(simbolo,ativo.name()==null?simbolo:ativo.name(),"B3",preco,fonte());
        } catch (ProviderCotacaoException ex) {
            throw ex;
        } catch (RestClientException|NumberFormatException ex) {
            throw new ProviderCotacaoException(fonte(),"Falha ao consultar Alpha Vantage",ex);
        }
    }
 
    private void validarErro(String information,String note,String error) {
        if (information==null&&note==null&&error==null) return;
        // O texto externo só é inspecionado: nunca integra a exceção ou o log.
        String texto=(Objects.toString(information,"")+" "+Objects.toString(note,"")+" "+Objects.toString(error,""))
            .toLowerCase(Locale.ROOT);
        if (texto.contains("invalid")&&texto.contains("key")) throw new ProviderCotacaoException(fonte(),AUTENTICACAO);
        if (texto.contains("rate")||texto.contains("limit")||texto.contains("frequency"))
            throw new ProviderCotacaoException(fonte(),LIMITE);
        if (texto.contains("premium")||texto.contains("subscription"))
            throw new ProviderCotacaoException(fonte(),PLANO);
        throw new ProviderCotacaoException(fonte(),ERRO_DO_PROVEDOR);
    }

    @JsonIgnoreProperties(ignoreUnknown=true)
    public record Busca(@JsonProperty("bestMatches") List<Correspondencia> matches,
        @JsonProperty("Information") String information,@JsonProperty("Note") String note,
        @JsonProperty("Error Message") String error) {}
    @JsonIgnoreProperties(ignoreUnknown=true)
    public record Correspondencia(@JsonProperty("1. symbol") String symbol,
        @JsonProperty("2. name") String name,@JsonProperty("8. currency") String currency) {}
    @JsonIgnoreProperties(ignoreUnknown=true)
    public record Resposta(@JsonProperty("Global Quote") Quote quote,
        @JsonProperty("Information") String information,@JsonProperty("Note") String note,
        @JsonProperty("Error Message") String error) {}
    @JsonIgnoreProperties(ignoreUnknown=true)
    public record Quote(@JsonProperty("05. price") String price) {}
}
