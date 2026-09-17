package com.curso.cotacao;
import com.curso.domains.enums.FonteCotacao;
public class ProviderCotacaoException extends RuntimeException {
    private final FonteCotacao fonte;
    private final String categoria;
    public enum Categoria { AUTENTICACAO, LIMITE, PLANO, BUSCA_SEM_ATIVO, COTACAO_AUSENTE, ERRO_DO_PROVEDOR }
    public ProviderCotacaoException(FonteCotacao fonte, Categoria categoria) {
        super("Falha em " + fonte + ": " + categoria.name());
        this.fonte=fonte;
        this.categoria=categoria.name();
    }
    public ProviderCotacaoException(FonteCotacao fonte,String mensagem) {
        super(mensagem); this.fonte=fonte; this.categoria="DADO_AUSENTE_OU_INVALIDO";
    }
    public ProviderCotacaoException(FonteCotacao fonte,String mensagem,Throwable causa) {
        // Não reter mensagens HTTP: podem conter corpo, URL e chave de acesso.
        super(mensagem); this.fonte=fonte; this.categoria=classificar(causa);
    }
    private static String classificar(Throwable causa) {
        if(causa instanceof org.springframework.web.client.RestClientResponseException http) {
            int status=http.getStatusCode().value();
            return status==401||status==403?"AUTENTICACAO":status==429?"LIMITE":"HTTP_"+status;
        }
        if(causa instanceof org.springframework.web.client.ResourceAccessException) return "TIMEOUT_OU_TRANSPORTE";
        return "RESPOSTA_INVALIDA";
    }
    public FonteCotacao getFonte(){return fonte;}
    public String getCategoria(){return categoria;}
}
