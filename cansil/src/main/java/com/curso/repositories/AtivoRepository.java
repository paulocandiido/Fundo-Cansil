package com.curso.repositories;
import com.curso.domains.Ativo; import org.springframework.data.jpa.repository.JpaRepository; import java.util.Optional;
public interface AtivoRepository extends JpaRepository<Ativo,Long>{
 Optional<Ativo> findBySimboloIgnoreCase(String simbolo);

 @org.springframework.data.jpa.repository.Query(value="select id from catalogo_ativo_lock where id=1 for update",nativeQuery=true)
 Integer bloquearCatalogo();

 @org.springframework.transaction.annotation.Transactional
 default Ativo obterOuCriar(String simbolo,String nome,String bolsa) {
  return obterOuCriar(simbolo,nome,bolsa,"BRL");
 }
 @org.springframework.transaction.annotation.Transactional
 default Ativo obterOuCriar(String simbolo,String nome,String bolsa,String moeda) {
  Optional<Ativo> existente=findBySimboloIgnoreCase(simbolo);
  if(existente.isPresent()) { com.curso.cotacao.Moedas.conferir(existente.get().getMoeda(),moeda); return existente.get(); }
  // Uma linha persistente permite bloquear também quando o ativo ainda não existe.
  if(bloquearCatalogo()==null) throw new IllegalStateException("Bloqueio do catálogo não inicializado");
  Ativo ativo=findBySimboloIgnoreCase(simbolo).orElseGet(()->save(new Ativo(null,simbolo,nome,bolsa,moeda)));
  com.curso.cotacao.Moedas.conferir(ativo.getMoeda(),moeda);
  return ativo;
 }
}
