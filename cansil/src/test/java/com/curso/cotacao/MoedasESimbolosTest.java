package com.curso.cotacao;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class MoedasESimbolosTest {
 @Test void permiteCodigosCurtosEClassesSemFixarPais(){
  for(String s:new String[]{"T","IBM","AAPL","BRK.B","BTC-USD","PETR4"}) assertEquals(s,Simbolos.normalizar(s.toLowerCase()));
 }
 @Test void rejeitaCodigosQueNaoSaoIdentificadores(){
  for(String s:new String[]{"","A/B","../ABC","A B","A?B","A&B","ABCDEFGHIJKLMNOPQRSTU"}) assertThrows(IllegalArgumentException.class,()->Simbolos.normalizar(s));
 }
 @Test void validaMoedasSemConversao(){
  assertEquals("USD",Moedas.normalizar("usd"));
  assertThrows(IllegalArgumentException.class,()->Moedas.normalizar("ZZZ"));
  assertThrows(IllegalArgumentException.class,()->Moedas.normalizar(null));
  assertThrows(com.curso.services.exceptions.RegraNegocioException.class,()->Moedas.conferir("BRL","USD"));
 }
}
