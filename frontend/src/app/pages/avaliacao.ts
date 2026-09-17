import { Component, DestroyRef, inject, signal } from '@angular/core';
import { DecimalPipe, DatePipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { finalize } from 'rxjs';
import { Api, ErroApi } from '../core/api';
import { Avaliacao } from '../core/modelos';

@Component({
  selector: 'app-avaliacao', imports: [DecimalPipe, DatePipe, RouterLink],
  template: `
    <section class="workspace"><div class="workspace-heading"><div><span class="eyebrow">COTAÇÕES E RESULTADOS</span><h1>Avaliação da carteira</h1><p class="muted">Compare o custo das posições com os preços informados pelos provedores.</p></div><button class="button button-primary" type="button" [disabled]="carregando()" (click)="avaliar()">{{ carregando() ? 'Avaliando…' : 'Avaliar carteira' }}</button></div>
      <p class="notice">A avaliação consulta cada ativo e consome a cota do provedor. Ela só é executada ao clicar no botão. Os preços podem ter atraso.</p>
      @if (erro()) { <p class="notice error" role="alert">{{ erro() }} Nenhuma avaliação parcial é apresentada. <a routerLink="/carteira">Ver posições sem cotação</a></p> }
      <section class="portfolio-panel" [attr.aria-busy]="carregando()">
        @if (carregando()) { <div class="empty-state" role="status"><span class="loading-dot" aria-hidden="true"></span><h2>Avaliando suas posições</h2><p>Isso pode levar alguns instantes.</p></div> }
        @else if (resultado(); as itens) {
          @if (itens.length === 0) { <div class="empty-state"><h2>Nenhuma posição aberta</h2><p>Registre compras para acompanhar os resultados.</p><a routerLink="/operacoes" class="button button-quiet">Registrar operação</a></div> }
          @else { <div class="table-scroll" tabindex="0" aria-label="Avaliação detalhada da carteira"><table class="valuation-table"><caption class="sr-only">Avaliação de cada posição, valores na moeda de cada ativo</caption><thead><tr><th>Ativo / fonte</th><th class="numeric">Quantidade</th><th class="numeric">Média</th><th class="numeric">Cotação</th><th class="numeric">Custo</th><th class="numeric">Valor atual</th><th class="numeric">Resultado</th></tr></thead><tbody>
            @for (item of itens; track item.simbolo) { <tr><th scope="row">{{ item.simbolo }}<small class="cell-detail">{{ item.fonte }} · {{ item.moeda || 'BRL' }}</small></th><td class="numeric">{{ item.quantidade | number:'1.0-6' }}</td><td class="numeric">{{ item.precoMedio | number:'1.2-6' }}</td><td class="numeric">{{ item.cotacaoAtual | number:'1.2-6' }}</td><td class="numeric">{{ item.custoTotal | number:'1.2-2' }}</td><td class="numeric">{{ item.valorAtual | number:'1.2-2' }}</td><td class="numeric" [class.negative]="item.lucroPrejuizo < 0" [class.positive]="item.lucroPrejuizo > 0">{{ item.moeda || 'BRL' }} {{ item.lucroPrejuizo | number:'1.2-2' }}<small class="cell-detail">{{ item.lucroPrejuizoPercentual | number:'1.2-2' }}%</small></td></tr> }
          </tbody></table></div> }
          <div class="panel-footer">Avaliação consultada às {{ consultadaEm() | date:'HH:mm:ss' }}. Esse horário não é o horário de mercado da cotação.</div>
        } @else { <div class="empty-state"><h2>{{ erro() ? 'Avaliação indisponível' : 'Consulte os resultados da carteira' }}</h2><p>Use “Avaliar carteira” quando desejar obter uma nova avaliação.</p></div> }
      </section><p class="portfolio-note">Cálculos feitos pelo backend. Resultado das posições abertas, sem impostos, taxas ou dividendos.</p>
    </section>
  `,
})
export class AvaliacaoPage {
  private readonly api = inject(Api); private readonly router = inject(Router); private readonly destroyRef = inject(DestroyRef);
  readonly carregando = signal(false); readonly erro = signal(''); readonly resultado = signal<Avaliacao[] | null>(null); readonly consultadaEm = signal<Date | null>(null);
  avaliar() {
    if (this.carregando()) return;
    this.carregando.set(true); this.erro.set(''); this.resultado.set(null); this.consultadaEm.set(null);
    this.api.avaliar().pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.carregando.set(false)))
      .subscribe({ next: itens => { this.resultado.set(itens); this.consultadaEm.set(new Date()); }, error: (erro: ErroApi) => { this.erro.set(erro.message); if (erro.status === 401) void this.router.navigate(['/login'], { queryParams: { sessao: 'expirada' } }); } });
  }
}
