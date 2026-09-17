import { Component, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { Api, ErroApi } from '../core/api';
import { Consulta, Cotacao } from '../core/modelos';
import { DataLocalPipe, simboloValido } from '../core/operacao-validadores';

@Component({
  selector: 'app-cotacoes', imports: [ReactiveFormsModule, DecimalPipe, RouterLink, DataLocalPipe],
  template: `
    <section class="workspace quotes-workspace">
      <div class="workspace-heading"><div><span class="eyebrow">MERCADO</span><h1>Cotações</h1><p class="muted">Consulte o preço atual antes de registrar uma operação.</p></div></div>
      <section class="portfolio-panel quote-panel"><div class="panel-body">
        <form class="quote-search" [formGroup]="form" (ngSubmit)="buscar()" novalidate [attr.aria-busy]="buscando()">
          <div><label for="simbolo-busca">Código do ativo</label><input id="simbolo-busca" formControlName="simbolo" placeholder="Ex.: PETR4" maxlength="20" autocapitalize="characters" [attr.aria-invalid]="form.controls.simbolo.touched && form.invalid" aria-describedby="busca-ajuda"></div>
          <button class="button button-primary" type="submit" [disabled]="buscando()">{{ buscando() ? 'Buscando…' : 'Buscar cotação' }}</button>
        </form>
        <p id="busca-ajuda" class="field-hint">Digite o código do ativo, como PETR4 ou AAPL.</p>
        @if (form.controls.simbolo.touched && form.invalid) { <p class="field-error" role="alert">Informe um código válido.</p> }
        @if (erro()) { <p class="notice error" role="alert">{{ erro() }}</p> }
        @if (buscando()) { <p role="status" class="muted">Buscando cotação…</p> }
        @if (resultado(); as cotacao) {
          <div class="quote-result" role="status"><div><span class="eyebrow">{{ cotacao.simbolo }}</span><h2>{{ cotacao.nome || cotacao.simbolo }}</h2><p class="muted">{{ cotacao.bolsa || 'Bolsa não informada' }} · Fonte: {{ cotacao.fonte }}</p></div><div class="quote-price"><span class="muted">Cotação atual</span><strong>{{ cotacao.moeda || 'BRL' }} {{ cotacao.preco | number:'1.2-6' }}</strong><a class="button button-primary" routerLink="/operacoes" [queryParams]="{simbolo: cotacao.simbolo, tipo: 'COMPRA', mercado: cotacao.moeda === 'USD' ? 'USA' : 'BR'}">Registrar operação</a></div></div>
        }
      </div></section>
      <section class="portfolio-panel spaced" aria-labelledby="historico-consultas"><div class="panel-heading"><div><h2 id="historico-consultas">Histórico de consultas</h2><span class="muted">Somente as consultas desta conta</span></div><button class="button button-quiet" type="button" [disabled]="carregando() || buscando()" (click)="carregarHistorico()">{{ carregando() ? 'Atualizando…' : 'Atualizar histórico' }}</button></div>
        @if (erroHistorico()) { <p class="notice error" role="alert">{{ erroHistorico() }} Os registros exibidos podem estar desatualizados.</p> }
        @if (carregando() && !carregado()) { <div class="empty-state" role="status">Carregando histórico…</div> }
        @else if (!carregado() && erroHistorico()) { <div class="empty-state">Histórico indisponível.</div> }
        @else if (historico().length === 0) { <div class="empty-state"><h3>Nenhuma consulta salva</h3><p>Busque a cotação de um ativo para começar.</p></div> }
        @else { <div class="table-scroll" tabindex="0" aria-label="Histórico de cotações"><table><caption class="sr-only">Cotações consultadas, mais recentes primeiro</caption><thead><tr><th>Data e hora</th><th>Ativo</th><th class="numeric">Preço / moeda</th><th>Fonte</th></tr></thead><tbody>@for (item of historico(); track item.id) { <tr><td>{{ item.consultadoEm | dataLocal }}</td><th scope="row">{{ item.simbolo }}</th><td class="numeric">{{ item.moeda || 'BRL' }} {{ item.valor | number:'1.2-6' }}</td><td>{{ item.fonte }}</td></tr> }</tbody></table></div> }
      </section>
    </section>
  `,
})
export class CotacoesPage implements OnInit {
  private readonly api = inject(Api);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private versao = 0;
  readonly form = inject(FormBuilder).nonNullable.group({ simbolo: [inject(ActivatedRoute).snapshot.queryParamMap.get('simbolo') ?? '', [Validators.required, simboloValido]] });
  readonly buscando = signal(false); readonly carregando = signal(false); readonly carregado = signal(false);
  readonly resultado = signal<Cotacao | null>(null); readonly historico = signal<Consulta[]>([]);
  readonly erro = signal(''); readonly erroHistorico = signal('');
  ngOnInit() { this.carregarHistorico(); }
  buscar() {
    if (this.buscando()) return;
    this.form.controls.simbolo.setValue(this.form.controls.simbolo.value.trim().toUpperCase());
    this.form.markAllAsTouched(); if (this.form.invalid) return;
    this.buscando.set(true); this.erro.set(''); this.resultado.set(null);
    this.api.cotacao(this.form.controls.simbolo.value).pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.buscando.set(false)))
      .subscribe({ next: cotacao => { this.resultado.set(cotacao); this.carregarHistorico(); }, error: (erro: ErroApi) => { this.erro.set(erro.message); this.autenticacao(erro); } });
  }
  carregarHistorico() {
    const versao = ++this.versao; this.carregando.set(true); this.erroHistorico.set('');
    this.api.historicoConsultas().pipe(takeUntilDestroyed(this.destroyRef), finalize(() => { if (versao === this.versao) this.carregando.set(false); }))
      .subscribe({ next: lista => { if (versao === this.versao) { this.historico.set(lista); this.carregado.set(true); } }, error: (erro: ErroApi) => { if (versao === this.versao) { this.erroHistorico.set(erro.message); this.autenticacao(erro); } } });
  }
  private autenticacao(erro: ErroApi) { if (erro.status === 401) void this.router.navigate(['/login'], { queryParams: { sessao: 'expirada' } }); }
}
