package com.curso.services;
import com.curso.cotacao.*;import com.curso.domains.*;import com.curso.domains.dtos.InvestimentoDTOs.*;import com.curso.domains.enums.Mercado;import com.curso.repositories.*;import com.curso.services.exceptions.RegraNegocioException;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import java.math.*;import java.time.*;import java.util.*;import java.util.stream.Collectors;
@Service public class TransacaoService{private final TransacaoRepository repo;private final AtivoRepository ativos;private final UsuarioAtualService atual;private final CalculadoraPosicao calc;private final CotacaoOrchestrator orq;private final CorretoraService corretoras;private final Clock relogio;public TransacaoService(TransacaoRepository r,AtivoRepository a,UsuarioAtualService u,CalculadoraPosicao c,CotacaoOrchestrator o,CorretoraService cs,Clock relogio){repo=r;ativos=a;atual=u;calc=c;orq=o;corretoras=cs;this.relogio=relogio;}
 @Transactional public TransacaoResponse registrar(TransacaoRequest d){
  if(d==null||d.tipo()==null||d.corretoraId()==null||d.corretoraId()<=0||!decimalValido(d.quantidade())||!decimalValido(d.valorUnitario()))throw new RegraNegocioException("Transação inválida: corretora e valores positivos com até 13 dígitos inteiros e 6 decimais");
  Usuario u=atual.bloquear();
  String s=Simbolos.normalizar(d.simbolo());
  Corretora corretora=corretoras.obterParaOperacao(d.corretoraId(),d.tipo());
  String moeda=d.mercado()==Mercado.USA?"USD":"BRL";
  Ativo a=ativos.findBySimboloIgnoreCase(s).orElseGet(()->{CotacaoAtual q=orq.buscar(s);Moedas.conferir(moeda,q.moeda());return "BRL".equals(q.moeda())?ativos.obterOuCriar(q.simbolo(),q.nome(),q.bolsa()):ativos.obterOuCriar(q.simbolo(),q.nome(),q.bolsa(),q.moeda());});
  Moedas.conferir(a.getMoeda(),moeda);
  List<Transacao> lista=new ArrayList<>(repo.findForUpdate(u.getId(),a.getId()));
  Transacao candidata=new Transacao(null,u,a,corretora,d.tipo(),d.quantidade(),d.valorUnitario(),LocalDateTime.now(relogio));
  lista.add(candidata);
  lista.sort(Comparator.comparing(Transacao::getDataOperacao).thenComparing(Transacao::getId,Comparator.nullsLast(Comparator.naturalOrder())));
  calc.calcular(lista.stream().filter(t->t.getCorretora().getId().equals(corretora.getId())).toList());
  calc.calcular(lista);
  return dto(repo.save(candidata));
 }
 private boolean decimalValido(BigDecimal valor){return valor!=null&&valor.signum()>0&&valor.scale()<=6&&valor.precision()-valor.scale()<=13;}
 @Transactional(readOnly=true) public List<TransacaoResponse> listar(){return repo.findByUsuarioIdOrderByDataOperacaoAscIdAsc(atual.obter().getId()).stream().map(this::dto).toList();}
 @Transactional(readOnly=true) public List<PosicaoResponse> posicoes(){return agrupar().entrySet().stream().map(e->{var p=calc.calcular(e.getValue());return new PosicaoResponse(e.getKey(),p.quantidade(),p.precoMedio(),custo(p),e.getValue().get(0).getAtivo().getMoeda());}).filter(p->p.quantidade().signum()>0).toList();}
 @Transactional(readOnly=true) public List<PosicaoCorretoraResponse> posicoesPorCorretora(){return repo.findByUsuarioIdOrderByDataOperacaoAscIdAsc(atual.obter().getId()).stream().collect(Collectors.groupingBy(t->new Chave(t.getAtivo().getSimbolo(),t.getCorretora()),LinkedHashMap::new,Collectors.toList())).entrySet().stream().map(e->{var p=calc.calcular(e.getValue());return new PosicaoCorretoraResponse(e.getKey().simbolo(),CorretoraService.dto(e.getKey().corretora()),p.quantidade(),p.precoMedio(),custo(p),e.getValue().get(0).getAtivo().getMoeda());}).filter(p->p.quantidade().signum()>0).sorted(Comparator.comparing(PosicaoCorretoraResponse::simbolo).thenComparing(p->p.corretora().nome())).toList();}
 @Transactional(readOnly=true) public List<AvaliacaoResponse> avaliar(){return posicoes().stream().map(p->{CotacaoAtual c=orq.buscar(p.simbolo());Moedas.conferir(p.moeda(),c.moeda());BigDecimal custo=p.custoTotal().setScale(2,RoundingMode.HALF_UP),valor=p.quantidade().multiply(c.preco()).setScale(2,RoundingMode.HALF_UP),lp=valor.subtract(custo),pct=custo.signum()==0?BigDecimal.ZERO:lp.multiply(BigDecimal.valueOf(100)).divide(custo,2,RoundingMode.HALF_UP);return new AvaliacaoResponse(p.simbolo(),p.quantidade(),p.precoMedio(),c.preco(),c.fonte(),custo,valor,lp,pct,p.moeda());}).toList();}
 private BigDecimal custo(CalculadoraPosicao.Posicao p){return p.custoTotal().setScale(2,RoundingMode.HALF_UP);}private Map<String,List<Transacao>> agrupar(){return repo.findByUsuarioIdOrderByDataOperacaoAscIdAsc(atual.obter().getId()).stream().collect(Collectors.groupingBy(t->t.getAtivo().getSimbolo(),LinkedHashMap::new,Collectors.toList()));}private TransacaoResponse dto(Transacao t){return new TransacaoResponse(t.getId(),t.getAtivo().getSimbolo(),CorretoraService.dto(t.getCorretora()),t.getTipo(),t.getQuantidade(),t.getValorUnitario(),t.getDataOperacao(),t.getAtivo().getMoeda());}private record Chave(String simbolo,Corretora corretora){}
}
