import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { Api, ErroApi } from '../core/api';
import { ConsultaCnpj, Corretora } from '../core/modelos';
import { cnpjValido, normalizarCnpj } from '../core/cnpj';
import { Sessao } from '../core/sessao';
import { OperacaoEmEnvio } from '../core/operacao-em-envio';

@Component({
  selector: 'app-corretoras',
  imports: [ReactiveFormsModule, RouterLink, DatePipe],
  templateUrl: './corretoras.html',
})
export class CorretorasPage implements OnInit {
  private readonly api = inject(Api);
  private readonly destroyRef = inject(DestroyRef);
  private readonly router = inject(Router);
  private readonly sessao = inject(Sessao);
  private versaoConsulta = 0;
  private loginPendente = false;
  readonly enviando = inject(OperacaoEmEnvio).ativa;
  readonly carregando = signal(false);
  readonly consultando = signal(false);
  readonly erro = signal('');
  readonly erroLista = signal('');
  readonly sucesso = signal('');
  readonly incerto = signal(false);
  readonly carregada = signal(false);
  readonly corretoras = signal<Corretora[]>([]);
  readonly corretorasAtivas = computed(() => this.corretoras().filter(c => c.ativa && !c.legada && !c.removida).length);
  readonly consulta = signal<ConsultaCnpj | null>(null);
  readonly legada = signal<Corretora | null>(null);
  readonly removendo = signal<Corretora | null>(null);
  readonly form = inject(FormBuilder).nonNullable.group({
    cnpj: ['', [Validators.required, c => cnpjValido(c.value) ? null : { cnpj: true }]],
  });
  constructor() {
    this.form.controls.cnpj.valueChanges.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      ++this.versaoConsulta; this.consulta.set(null); this.consultando.set(false); this.erro.set(''); this.sucesso.set('');
    });
  }
  ngOnInit() { this.carregar(); }
  carregar() {
    if (this.carregando() || this.enviando()) return;
    this.carregando.set(true); this.erroLista.set('');
    this.api.corretoras().pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.carregando.set(false))).subscribe({
      next: lista => { this.corretoras.set(lista); this.carregada.set(true); this.incerto.set(false); this.removendo.set(null); },
      error: (e: ErroApi) => { this.erroLista.set(e.message); this.autenticacao(e); },
    });
  }
  consultar() {
    this.form.controls.cnpj.markAsTouched();
    if (this.form.controls.cnpj.invalid || this.enviando() || this.consultando()) return;
    const versao = ++this.versaoConsulta;
    const cnpj = normalizarCnpj(this.form.controls.cnpj.value);
    this.consultando.set(true); this.consulta.set(null); this.erro.set('');
    this.api.consultarCnpj(cnpj).pipe(takeUntilDestroyed(this.destroyRef), finalize(() => {
      if (versao === this.versaoConsulta) this.consultando.set(false);
    })).subscribe({
      next: dados => {
        if (versao !== this.versaoConsulta) return;
        if (dados.cnpj !== cnpj || !dados.razaoSocial?.trim()) { this.erro.set('A consulta não confirmou o CNPJ informado. Tente novamente.'); return; }
        this.consulta.set(dados);
      },
      error: (e: ErroApi) => { if (versao === this.versaoConsulta) this.erro.set(e.message); this.autenticacao(e); },
    });
  }
  regularizar(corretora: Corretora) {
    if (this.enviando() || this.incerto() || !corretora.legada) return;
    this.legada.set(corretora);
    this.form.reset({ cnpj: '' });
  }
  cancelarRegularizacao() {
    if (this.enviando() || this.incerto()) return;
    this.legada.set(null); this.form.enable(); this.form.reset({ cnpj: '' });
  }
  salvar() {
    this.form.markAllAsTouched();
    const empresa = this.consulta(), dados = this.form.getRawValue();
    if (this.form.invalid || !empresa || empresa.cnpj !== normalizarCnpj(dados.cnpj)
        || this.enviando() || this.incerto() || this.carregando() || !this.carregada()) return;
    this.enviando.set(true); this.form.disable({ emitEvent: false }); this.erro.set(''); this.sucesso.set('');
    this.api.cadastrarCorretora({ cnpj: empresa.cnpj,
      ...(this.legada() ? { corretoraLegadaId: this.legada()!.id } : {}) }).pipe(
        takeUntilDestroyed(this.destroyRef), finalize(() => {
          this.enviando.set(false); this.form.enable({ emitEvent: false });
          this.encaminharAoLogin();
        }),
      ).subscribe({
      next: corretora => {
        this.corretoras.update(lista => [...lista.filter(c => c.id !== corretora.id), corretora].sort((a,b) => a.nome.localeCompare(b.nome)));
        this.legada.set(null); this.form.reset({ cnpj: '' });
        this.sucesso.set('Corretora cadastrada: ' + corretora.nome + '.');
      },
      error: (e: ErroApi) => { this.erro.set(e.message); this.incerto.set(e.resultadoIncerto); this.autenticacao(e); },
    });
  }
  solicitarRemocao(corretora: Corretora) {
    if (this.enviando() || this.carregando() || this.incerto()) return;
    this.removendo.set(corretora); this.erro.set(''); this.sucesso.set('');
  }
  cancelarRemocao() { if (!this.enviando()) this.removendo.set(null); }
  confirmarRemocao() {
    const corretora = this.removendo();
    if (!corretora || this.enviando() || this.incerto() || this.carregando()) return;
    this.enviando.set(true); this.form.disable({ emitEvent: false });
    this.api.removerCorretora(corretora.id).pipe(takeUntilDestroyed(this.destroyRef), finalize(() => {
      this.enviando.set(false); this.form.enable({ emitEvent: false }); this.encaminharAoLogin();
    })).subscribe({
      next: () => {
        this.corretoras.update(lista => lista.filter(c => c.id !== corretora.id));
        if (this.legada()?.id === corretora.id) { this.legada.set(null); this.form.reset({ cnpj: '' }); }
        this.removendo.set(null);
        this.sucesso.set('Corretora e operações relacionadas excluídas.');
      },
      error: (e: ErroApi) => { this.erro.set(e.message); this.incerto.set(e.resultadoIncerto); this.autenticacao(e); },
    });
  }
  private autenticacao(e: ErroApi) {
    if (e.status === 401 && !this.sessao.obterToken()) {
      this.loginPendente = true;
      this.encaminharAoLogin();
    }
  }
  private encaminharAoLogin() {
    if (!this.loginPendente || this.enviando() || this.destroyRef.destroyed) return;
    this.loginPendente = false;
    void this.router.navigate(['/login'], { queryParams: { sessao: 'expirada' } });
  }
}
