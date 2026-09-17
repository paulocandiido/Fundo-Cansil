import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { finalize, startWith } from 'rxjs';
import { Api, ErroApi } from '../core/api';
import { AtivoCatalogo, Corretora, Mercado, NovaTransacao, PosicaoCorretora, TipoTransacao, Transacao } from '../core/modelos';
import { OperacaoEmEnvio } from '../core/operacao-em-envio';
import { Sessao } from '../core/sessao';
import { DataLocalPipe, decimalParaApi, decimalPositivo, simboloValido } from '../core/operacao-validadores';

@Component({
  selector: 'app-operacoes', imports: [ReactiveFormsModule, DecimalPipe, DataLocalPipe, RouterLink],
  templateUrl: './operacoes.html',
})
export class OperacoesPage implements OnInit {
  private readonly api = inject(Api);
  private readonly router = inject(Router);
  private readonly sessao = inject(Sessao);
  private readonly destroyRef = inject(DestroyRef);
  private readonly params = inject(ActivatedRoute).snapshot.queryParamMap;
  private versaoHistorico = 0;
  private versaoBusca = 0;
  private versaoCotacao = 0;
  private atalhoPendente = true;
  private loginPendente = false;
  readonly enviando = inject(OperacaoEmEnvio).ativa;
  readonly carregando = signal(false);
  readonly erro = signal('');
  readonly erroHistorico = signal('');
  readonly sucesso = signal('');
  readonly incerto = signal(false);
  readonly historicoConferivel = signal(false);
  readonly carregado = signal(false);
  readonly transacoes = signal<Transacao[]>([]);
  readonly corretoras = signal<Corretora[]>([]);
  readonly posicoesProprias = signal<PosicaoCorretora[]>([]);
  readonly ativosEncontrados = signal<AtivoCatalogo[]>([]);
  readonly buscaAtivo = signal(this.params.get('simbolo') ?? '');
  readonly carregandoAtivos = signal(false);
  readonly erroCatalogo = signal('');
  readonly carregandoCotacao = signal(false);
  readonly erroCotacao = signal('');
  readonly erroApoio = signal('');
  readonly paginaAtivos = signal(0);
  readonly totalAtivos = signal(0);
  readonly tamanhoPagina = 20;
  readonly catalogoDesatualizado = signal(false);
  readonly form = inject(FormBuilder).nonNullable.group({
    mercado: [this.params.get('mercado') === 'USA' ? 'USA' as Mercado : 'BR' as Mercado, [Validators.required, Validators.pattern(/^(BR|USA)$/)]],
    simbolo: ['', [Validators.required, simboloValido]],
    corretoraId: [0, [Validators.required, Validators.min(1)]],
    tipo: [this.params.get('tipo') === 'VENDA' ? 'VENDA' as TipoTransacao : 'COMPRA' as TipoTransacao, [Validators.required, Validators.pattern(/^(COMPRA|VENDA)$/)]],
    quantidade: ['', [Validators.required, decimalPositivo]],
    valorUnitario: ['', [Validators.required, decimalPositivo]],
  });
  readonly mercado = toSignal(this.form.controls.mercado.valueChanges.pipe(startWith(this.form.controls.mercado.value)),{initialValue:this.form.controls.mercado.value});
  readonly moeda = computed(()=>this.mercado() === 'USA' ? 'USD' : 'BRL');
  readonly ativosProprios = computed(()=>[...new Set(this.posicoesProprias().filter(p=>(p.moeda ?? 'BRL')===this.moeda()).map(p=>p.simbolo))].sort());
  codigoInformado(){const s=this.buscaAtivo().trim().toUpperCase();return /^[A-Z0-9][A-Z0-9.-]{0,19}$/.test(s)&&!this.ativosEncontrados().some(a=>a.simbolo===s)&&!this.ativosProprios().includes(s)?s:'';}
  ativosPropriosAlternativos(){const encontrados=new Set(this.ativosEncontrados().map(a=>a.simbolo));return this.ativosProprios().filter(s=>!encontrados.has(s));}
  corretorasPermitidas(){const tipo=this.form.controls.tipo.value,simbolo=this.form.controls.simbolo.value;if(tipo==='COMPRA')return this.corretoras().filter(c=>!c.removida&&c.ativa&&!c.legada&&!!c.cnpj&&!!c.verificadoEm);const posicoes=this.posicoesProprias().filter(p=>(p.moeda??'BRL')===this.moeda()&&(!simbolo||p.simbolo===simbolo));return [...new Map(posicoes.map(p=>[p.corretora.id,p.corretora])).values()].sort((a,b)=>a.nome.localeCompare(b.nome)||a.id-b.id);}
  ngOnInit() { this.carregarHistorico(); this.carregarApoio(); if(this.buscaAtivo())this.buscarAtivos(); }
  invalido(campo: keyof typeof this.form.controls) { const c = this.form.controls[campo]; return c.touched && c.invalid; }
  carregarHistorico() {
    const versao = ++this.versaoHistorico;
    this.carregando.set(true); this.erroHistorico.set(''); this.historicoConferivel.set(false);
    this.api.transacoes().pipe(takeUntilDestroyed(this.destroyRef), finalize(() => { if (versao === this.versaoHistorico) this.carregando.set(false); }))
      .subscribe({ next: lista => {
        if (versao !== this.versaoHistorico) return;
        this.transacoes.set([...lista].sort((a, b) => b.dataOperacao.localeCompare(a.dataOperacao) || b.id - a.id));
        this.carregado.set(true); this.historicoConferivel.set(true);
      }, error: (erro: ErroApi) => {
        if (versao === this.versaoHistorico) this.erroHistorico.set(erro.message);
        this.autenticacao(erro);
      } });
  }
  liberarReenvio() {
    if (this.incerto() && this.historicoConferivel() && !this.carregando()) { this.incerto.set(false); this.erro.set(''); }
  }
  corretoraSelecionada(){return this.corretorasPermitidas().find(c=>c.id===Number(this.form.controls.corretoraId.value));}
  alterarBusca(valor:string){
    this.atalhoPendente=false; ++this.versaoBusca; this.carregandoAtivos.set(false);
    this.buscaAtivo.set(valor.toUpperCase()); this.form.controls.simbolo.setValue('');
    this.limparCotacaoSelecionada();
    this.ativosEncontrados.set([]);this.totalAtivos.set(0);this.erroCatalogo.set('');
  }
  buscarAtivos(pagina=0){
    if(this.enviando())return;
    const versao=++this.versaoBusca, busca=this.buscaAtivo().trim().toUpperCase();
    this.buscaAtivo.set(busca);this.form.controls.simbolo.setValue('');
    this.limparCotacaoSelecionada();
    if(!/^[A-Z0-9.-]{0,20}$/.test(busca)){this.erroCatalogo.set('Use até 20 letras, números, pontos ou hífens na busca.');return;}
    if(this.form.controls.mercado.value==='USA'){
      this.carregandoAtivos.set(false);this.erroCatalogo.set('');this.ativosEncontrados.set([]);this.paginaAtivos.set(0);this.totalAtivos.set(0);this.catalogoDesatualizado.set(false);
      return;
    }
    this.carregandoAtivos.set(true);this.erroCatalogo.set('');
    this.api.ativos(busca,pagina,this.tamanhoPagina).pipe(takeUntilDestroyed(this.destroyRef),
      finalize(()=>{if(versao===this.versaoBusca)this.carregandoAtivos.set(false);})).subscribe({
      next:r=>{
        if(versao!==this.versaoBusca)return;
        this.ativosEncontrados.set(r.itens);this.paginaAtivos.set(r.pagina);this.totalAtivos.set(r.total);this.catalogoDesatualizado.set(r.desatualizado);
        const pretendido=this.params.get('simbolo')?.trim().toUpperCase();
        if(this.atalhoPendente&&pretendido&&r.itens.some(i=>i.simbolo===pretendido))this.form.controls.simbolo.setValue(pretendido);
        this.tentarAtalho();
      },
      error:(e:ErroApi)=>{if(versao===this.versaoBusca)this.erroCatalogo.set(e.status===503?'O catálogo está indisponível. Você ainda pode selecionar um ativo da sua carteira.':e.message);this.autenticacao(e);}
    });
  }
  selecionarAtivo(valor:string){
    const simbolo=valor.trim().toUpperCase();
    this.atalhoPendente=false;this.form.controls.simbolo.setValue(simbolo);
    this.limparCotacaoSelecionada();
    if(!this.corretorasPermitidas().some(c=>c.id===this.form.controls.corretoraId.value))this.form.controls.corretoraId.setValue(0);
    if(!simbolo)return;
    const versao=++this.versaoCotacao;
    this.carregandoCotacao.set(true);
    this.api.cotacao(simbolo).pipe(takeUntilDestroyed(this.destroyRef),finalize(()=>{
      if(versao===this.versaoCotacao)this.carregandoCotacao.set(false);
    })).subscribe({
      next:cotacao=>{
        if(versao!==this.versaoCotacao||this.form.controls.simbolo.value!==simbolo)return;
        if(this.form.controls.valorUnitario.value==='')this.form.controls.valorUnitario.setValue(this.precoParaFormulario(cotacao.preco));
      },
      error:(e:ErroApi)=>{
        if(versao===this.versaoCotacao)this.erroCotacao.set(e.status===401?e.message:`${e.message} Informe o preço da operação manualmente.`);
        this.autenticacao(e);
      }
    });
  }
  mudouCorretora(){
    this.atalhoPendente=false;
  }
  selecionarTipo(tipo:TipoTransacao){
    if(this.form.controls.tipo.value===tipo)return;
    this.form.controls.tipo.setValue(tipo);this.mudouTipo();
  }
  mudouTipo(){this.atalhoPendente=false;this.form.controls.corretoraId.setValue(0);}
  mudouMercado(){
    this.atalhoPendente=false;++this.versaoBusca;this.carregandoAtivos.set(false);this.form.controls.simbolo.setValue('');this.form.controls.corretoraId.setValue(0);
    this.limparCotacaoSelecionada();
    this.buscaAtivo.set('');this.ativosEncontrados.set([]);this.paginaAtivos.set(0);this.totalAtivos.set(0);this.catalogoDesatualizado.set(false);this.erroCatalogo.set('');
  }
  private limparCotacaoSelecionada(){
    ++this.versaoCotacao;this.carregandoCotacao.set(false);this.erroCotacao.set('');this.form.controls.valorUnitario.setValue('');
  }
  private precoParaFormulario(preco:number){
    return Number(preco).toFixed(6).replace(/0+$/,'').replace(/\.$/,'').replace('.',',');
  }
  valorTotalPrevisto(){
    const quantidade=Number(this.form.controls.quantidade.value.trim().replace(',','.'));
    const preco=Number(this.form.controls.valorUnitario.value.trim().replace(',','.'));
    return Number.isFinite(quantidade)&&Number.isFinite(preco)&&quantidade>0&&preco>0?quantidade*preco:0;
  }
  private carregarApoio(){
    this.api.corretoras().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next:c=>{this.corretoras.set(c);this.tentarAtalho();},error:(e:ErroApi)=>{this.erroApoio.set(e.message);this.autenticacao(e);}
    });
    this.api.carteiraPorCorretora().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next:p=>{this.posicoesProprias.set(p);this.tentarAtalho();},error:(e:ErroApi)=>{this.erroApoio.set(e.message);this.autenticacao(e);}
    });
  }
  private tentarAtalho(){
    if(!this.atalhoPendente)return;
    const id=Number(this.params.get('corretoraId'));
    if(!Number.isInteger(id)||id<=0)return;
    const corretora=this.corretoras().find(c=>c.id===id) ?? this.posicoesProprias().find(p=>p.corretora.id===id)?.corretora;
    if(!corretora)return;
    if(this.params.get('tipo')!=='VENDA'){
      if(!corretora.removida&&corretora.ativa&&!corretora.legada&&corretora.cnpj&&corretora.verificadoEm){this.form.controls.corretoraId.setValue(id);this.atalhoPendente=false;}
      return;
    }
    const simbolo=this.params.get('simbolo')?.trim().toUpperCase();
    const posicao=this.posicoesProprias().find(p=>p.simbolo===simbolo&&p.corretora.id===id);
    if(posicao){
      this.form.controls.mercado.setValue((posicao.moeda??'BRL')==='USD'?'USA':'BR');
      this.form.controls.simbolo.setValue(simbolo!);this.form.controls.corretoraId.setValue(id);this.atalhoPendente=false;
    }
  }
  enviar() {
    if (this.enviando() || this.incerto()) return;
    this.form.controls.simbolo.setValue(this.form.controls.simbolo.value.trim().toUpperCase());
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    if (!this.corretorasPermitidas().some(c=>c.id===Number(this.form.controls.corretoraId.value))) { this.erro.set('Selecione uma corretora cadastrada e disponível para esta operação.'); return; }
    const valor = this.form.getRawValue();
    const dados: NovaTransacao = { ...valor, corretoraId:Number(valor.corretoraId), quantidade: decimalParaApi(valor.quantidade), valorUnitario: decimalParaApi(valor.valorUnitario) };
    this.enviando.set(true); this.erro.set(''); this.sucesso.set(''); this.form.disable();
    this.api.registrarTransacao(dados).pipe(takeUntilDestroyed(this.destroyRef), finalize(() => {
      this.enviando.set(false); this.form.enable(); this.encaminharAoLogin();
    }))
      .subscribe({ next: registrada => {
        this.sucesso.set(`${registrada.tipo === 'COMPRA' ? 'Compra' : 'Venda'} de ${registrada.simbolo} em ${registrada.corretora.nome} registrada. Operação #${registrada.id}.`);
        this.form.reset({ mercado:'BR', simbolo: '', corretoraId:0, tipo: 'COMPRA', quantidade: '', valorUnitario: '' });this.buscaAtivo.set('');this.ativosEncontrados.set([]);
        if (!this.loginPendente) { this.carregarHistorico(); this.carregarApoio(); }
      }, error: (erro: ErroApi) => {
        if (erro.resultadoIncerto) { ++this.versaoHistorico; this.carregando.set(false); }
        this.erro.set(erro.message); this.incerto.set(erro.resultadoIncerto); this.historicoConferivel.set(false); this.autenticacao(erro);
      } });
  }
  private autenticacao(erro: ErroApi) {
    if (erro.status !== 401 || this.sessao.obterToken()) return;
    this.loginPendente = true;
    this.encaminharAoLogin();
  }
  private encaminharAoLogin() {
    // Não deixe a proteção de navegação descartar o redirecionamento por sessão expirada.
    if (!this.loginPendente || this.enviando() || this.destroyRef.destroyed) return;
    this.loginPendente = false;
    void this.router.navigate(['/login'], { queryParams: { sessao: 'expirada' } });
  }
}
