package com.curso.domains.dtos;
import com.curso.domains.enums.*;import jakarta.validation.constraints.*;import java.math.BigDecimal;import java.time.*;import java.util.List;
public final class InvestimentoDTOs{private InvestimentoDTOs(){}
 public record CotacaoResponse(String simbolo,String nome,String bolsa,BigDecimal preco,FonteCotacao fonte,String moeda){}
 public record ConsultaResponse(Long id,String simbolo,BigDecimal valor,FonteCotacao fonte,LocalDateTime consultadoEm,String moeda){}
 public record CorretoraResponse(Long id,String codigo,String nome,boolean ativa,boolean legada,String cnpj,Mercado mercado,Instant verificadoEm,boolean removida){
  public CorretoraResponse(Long id,String codigo,String nome,boolean ativa,boolean legada,String cnpj,Mercado mercado,Instant verificadoEm){this(id,codigo,nome,ativa,legada,cnpj,mercado,verificadoEm,false);}
 }
 public record CnpjResponse(String cnpj,String razaoSocial,String situacaoCadastral,boolean ativa,Instant consultadoEm){}
 // Mercado é aceito apenas para compatibilidade com clientes antigos; não vincula nem restringe operações.
 public record CorretoraRequest(@NotBlank @Size(max=18) String cnpj,Mercado mercado,@Positive Long corretoraLegadaId){}
 public record TransacaoRequest(@NotBlank String simbolo,@NotNull @Positive Long corretoraId,@NotNull TipoTransacao tipo,@NotNull @Digits(integer=13,fraction=6) @DecimalMin(value="0.0",inclusive=false) BigDecimal quantidade,@NotNull @Digits(integer=13,fraction=6) @DecimalMin(value="0.0",inclusive=false) BigDecimal valorUnitario,@NotNull Mercado mercado){
  public TransacaoRequest(String simbolo,Long corretoraId,TipoTransacao tipo,BigDecimal quantidade,BigDecimal valorUnitario,LocalDateTime ignorada){this(simbolo,corretoraId,tipo,quantidade,valorUnitario,Mercado.BR);}
 }
 public record TransacaoResponse(Long id,String simbolo,CorretoraResponse corretora,TipoTransacao tipo,BigDecimal quantidade,BigDecimal valorUnitario,LocalDateTime dataOperacao,String moeda){}
 public record PosicaoResponse(String simbolo,BigDecimal quantidade,BigDecimal precoMedio,BigDecimal custoTotal,String moeda){}
 public record PosicaoCorretoraResponse(String simbolo,CorretoraResponse corretora,BigDecimal quantidade,BigDecimal precoMedio,BigDecimal custoTotal,String moeda){}
 public record AvaliacaoResponse(String simbolo,BigDecimal quantidade,BigDecimal precoMedio,BigDecimal cotacaoAtual,FonteCotacao fonte,BigDecimal custoTotal,BigDecimal valorAtual,BigDecimal lucroPrejuizo,BigDecimal lucroPrejuizoPercentual,String moeda){}
 public record AtivoCatalogoResponse(String simbolo){}
 public record PaginaAtivosResponse(List<AtivoCatalogoResponse> itens,int pagina,int tamanho,long total,Instant atualizadoEm,boolean desatualizado){}
}
