package com.curso.cotacao;
import com.curso.config.CotacaoProperties; import com.curso.domains.enums.FonteCotacao; import com.fasterxml.jackson.annotation.JsonIgnoreProperties; import org.springframework.stereotype.Component; import org.springframework.web.client.*; import java.math.BigDecimal; import java.util.List;
@Component
@org.springframework.core.annotation.Order(10)
public class BrapiProvider implements CotacaoProvider {
 private final RestClient client; private final String token;
 public BrapiProvider(RestClient.Builder builder,CotacaoProperties p){this.client=builder.baseUrl(p.brapi().baseUrl()).build();this.token=p.brapi().token();}
 public FonteCotacao fonte(){return FonteCotacao.BRAPI;}
 public CotacaoAtual buscar(String valor){String s=Simbolos.normalizar(valor);try{Resposta r=client.get().uri(u->u.path("/api/v2/stocks/quote").queryParam("symbols",s).build()).headers(h->h.setBearerAuth(token)).retrieve().body(Resposta.class);if(r==null||r.results()==null||r.results().isEmpty()||r.results().get(0)==null||r.results().get(0).data()==null)throw new ProviderCotacaoException(fonte(),"BRAPI sem cotação para "+s);Item i=r.results().get(0);Dado d=i.data();String devolvido=d.symbol()!=null?d.symbol():i.symbol();if(devolvido==null||!s.equals(Simbolos.normalizar(devolvido)))throw new ProviderCotacaoException(fonte(),"BRAPI devolveu um ativo diferente do solicitado");if(d.regularMarketPrice()==null||d.regularMarketPrice().signum()<=0)throw new ProviderCotacaoException(fonte(),"BRAPI sem cotação para "+s);return new CotacaoAtual(s,d.shortName()==null?s:d.shortName(),"B3",d.regularMarketPrice(),fonte(),d.currency()==null?"BRL":d.currency());}catch(ProviderCotacaoException e){throw e;}catch(RestClientException|IllegalArgumentException e){throw new ProviderCotacaoException(fonte(),"Falha ao consultar BRAPI",e);}}
 @JsonIgnoreProperties(ignoreUnknown=true) public record Resposta(List<Item> results){} @JsonIgnoreProperties(ignoreUnknown=true) public record Item(String symbol,Dado data){} @JsonIgnoreProperties(ignoreUnknown=true) public record Dado(String symbol,String shortName,BigDecimal regularMarketPrice,String currency){}
}
