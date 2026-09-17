package com.curso.cotacao;
import com.curso.domains.enums.FonteCotacao; import java.math.BigDecimal;
public record CotacaoAtual(String simbolo,String nome,String bolsa,BigDecimal preco,FonteCotacao fonte,String moeda) {
 public CotacaoAtual { moeda=Moedas.normalizar(moeda); }
 public CotacaoAtual(String simbolo,String nome,String bolsa,BigDecimal preco,FonteCotacao fonte){this(simbolo,nome,bolsa,preco,fonte,"BRL");}
}
