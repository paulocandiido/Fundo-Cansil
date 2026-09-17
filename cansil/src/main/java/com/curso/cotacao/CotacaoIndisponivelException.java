package com.curso.cotacao;
import java.util.List;
public class CotacaoIndisponivelException extends RuntimeException { private final List<String> tentativas; public CotacaoIndisponivelException(String simbolo,List<String> tentativas){super("Cotação indisponível para "+simbolo+". Fontes tentadas: "+String.join("; ",tentativas));this.tentativas=List.copyOf(tentativas);} public List<String> getTentativas(){return tentativas;} }
