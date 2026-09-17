import { Component, computed, DestroyRef, inject, OnInit, signal } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { finalize, forkJoin } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Api, ErroApi } from '../core/api';
import { Avaliacao, Posicao, TipoTransacao, Transacao } from '../core/modelos';
import { DataLocalPipe } from '../core/operacao-validadores';

type FiltroTipo = 'TODAS' | TipoTransacao;

interface LinhaExtrato {
  operacao: Transacao;
  valorTotal: number;
  precoMedio: number | null;
  lucroPrejuizo: number | null;
}

interface ResumoMoeda {
  moeda: string;
  precoMedio: number;
  custoTotal: number;
  valorAtual: number | null;
}

@Component({
  selector: 'app-carteira',
  imports: [DatePipe, DecimalPipe, DataLocalPipe, RouterLink],
  templateUrl: './carteira.html',
})
export class CarteiraPage implements OnInit {
  private readonly api = inject(Api);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  readonly carregando = signal(false);
  readonly carregandoCotacoes = signal(false);
  readonly erro = signal('');
  readonly erroCotacoes = signal('');
  readonly posicoes = signal<Posicao[]>([]);
  readonly transacoes = signal<Transacao[]>([]);
  readonly cotacoes = signal(new Map<string, Avaliacao>());
  readonly atualizadaEm = signal<Date | null>(null);
  readonly consultaEm = signal<Date | null>(null);
  readonly filtroTipo = signal<FiltroTipo>('TODAS');
  readonly ativoEscolhido = signal('');
  readonly filtroAtivo = signal('');
  readonly identificador = signal('');
  readonly mensagemConsulta = signal('');
  readonly operacaoConsultada = signal<LinhaExtrato | null>(null);

  readonly totalAcoes = computed(() => this.posicoes().reduce((total, posicao) => total + Number(posicao.quantidade), 0));
  readonly ativosHistorico = computed(() => [...new Set(this.transacoes().map(item => item.simbolo))].sort());
  readonly extrato = computed(() => this.calcularExtrato(this.transacoes()));
  readonly extratoFiltrado = computed(() => this.extrato().filter(linha =>
    (this.filtroTipo() === 'TODAS' || linha.operacao.tipo === this.filtroTipo()) &&
    (!this.filtroAtivo() || linha.operacao.simbolo === this.filtroAtivo())));
  readonly resumos = computed<ResumoMoeda[]>(() => {
    const grupos = new Map<string, Posicao[]>();
    for (const posicao of this.posicoes()) {
      const moeda = this.moeda(posicao);
      grupos.set(moeda, [...(grupos.get(moeda) ?? []), posicao]);
    }
    return [...grupos.entries()].sort(([a], [b]) => a.localeCompare(b)).map(([moeda, posicoes]) => {
      const quantidade = posicoes.reduce((total, item) => total + Number(item.quantidade), 0);
      // Média ponderada: soma(qtd atual × preço médio do ativo) ÷ soma(qtd atual).
      const custoTotal = posicoes.reduce((total, item) => total + Number(item.custoTotal), 0);
      const todasCotadas = posicoes.every(item => this.cotacoes().has(item.simbolo));
      const valorAtual = todasCotadas
        ? posicoes.reduce((total, item) => total + Number(item.quantidade) * this.cotacoes().get(item.simbolo)!.cotacaoAtual, 0)
        : null;
      return { moeda, precoMedio: quantidade > 0 ? custoTotal / quantidade : 0, custoTotal, valorAtual };
    });
  });

  ngOnInit() { this.carregar(); }

  carregar() {
    if (this.carregando()) return;
    this.carregando.set(true); this.erro.set('');
    forkJoin({ posicoes: this.api.carteira(), transacoes: this.api.transacoes() })
      .pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.carregando.set(false)))
      .subscribe({
        next: dados => {
          this.posicoes.set(dados.posicoes);
          this.transacoes.set([...dados.transacoes].sort((a, b) => b.dataOperacao.localeCompare(a.dataOperacao) || b.id - a.id));
          this.atualizadaEm.set(new Date());
          if (this.identificador().trim()) this.consultarPorId();
        },
        error: (erro: ErroApi) => {
          if (erro.status === 401) {
            this.posicoes.set([]); this.transacoes.set([]);
            void this.router.navigate(['/login'], { queryParams: { sessao: 'expirada' } });
          } else this.erro.set(erro.message + (this.atualizadaEm() ? ' Os dados exibidos são da última atualização bem-sucedida.' : ''));
        },
      });
  }

  atualizarCotacoes() {
    if (this.carregandoCotacoes() || this.posicoes().length === 0) return;
    this.carregandoCotacoes.set(true); this.erroCotacoes.set(''); this.cotacoes.set(new Map()); this.consultaEm.set(null);
    this.api.avaliar().pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.carregandoCotacoes.set(false)))
      .subscribe({
        next: itens => { this.cotacoes.set(new Map(itens.map(item => [item.simbolo, item]))); this.consultaEm.set(new Date()); },
        error: (erro: ErroApi) => {
          if (erro.status === 401) void this.router.navigate(['/login'], { queryParams: { sessao: 'expirada' } });
          else this.erroCotacoes.set(erro.message);
        },
      });
  }

  selecionarTipo(tipo: FiltroTipo) { this.filtroTipo.set(tipo); }
  alterarAtivo(valor: string) { this.ativoEscolhido.set(valor); }
  filtrarHistorico() { this.filtroAtivo.set(this.ativoEscolhido()); }
  limparAtivo() { this.ativoEscolhido.set(''); this.filtroAtivo.set(''); }

  consultarPorId() {
    const valor = this.identificador().trim();
    this.operacaoConsultada.set(null);
    if (!valor) { this.mensagemConsulta.set('Informe um identificador.'); return; }
    if (!/^\d+$/.test(valor) || Number(valor) <= 0 || !Number.isSafeInteger(Number(valor))) {
      this.mensagemConsulta.set('Informe um identificador numérico válido.'); return;
    }
    const encontrada = this.extrato().find(linha => linha.operacao.id === Number(valor)) ?? null;
    this.operacaoConsultada.set(encontrada);
    this.mensagemConsulta.set(encontrada ? '' : 'Operação não encontrada');
  }

  moeda(posicao: Posicao) { return posicao.moeda ?? 'BRL'; }
  cotacaoAtual(posicao: Posicao) { return this.cotacoes().get(posicao.simbolo)?.cotacaoAtual ?? null; }
  valorAtual(posicao: Posicao) { const cotacao = this.cotacaoAtual(posicao); return cotacao === null ? null : Number(posicao.quantidade) * cotacao; }
  lucroAtual(posicao: Posicao) { const valor = this.valorAtual(posicao); return valor === null ? null : valor - Number(posicao.custoTotal); }
  parametrosVenda(posicao: Posicao) { return { simbolo: posicao.simbolo, tipo: 'VENDA', mercado: this.moeda(posicao) === 'USD' ? 'USA' : 'BR' }; }

  private calcularExtrato(transacoes: Transacao[]): LinhaExtrato[] {
    const estados = new Map<string, { quantidade: number; custo: number }>();
    return [...transacoes]
      .sort((a, b) => a.dataOperacao.localeCompare(b.dataOperacao) || a.id - b.id)
      .map(operacao => {
        const chave = `${operacao.simbolo}:${operacao.moeda ?? 'BRL'}`;
        const estado = estados.get(chave) ?? { quantidade: 0, custo: 0 };
        const quantidade = Number(operacao.quantidade);
        const preco = Number(operacao.valorUnitario);
        const mediaAnterior = estado.quantidade > 0 ? estado.custo / estado.quantidade : 0;
        let precoMedio: number | null;
        let lucroPrejuizo: number | null = null;
        if (operacao.tipo === 'COMPRA') {
          estado.quantidade += quantidade;
          estado.custo += quantidade * preco;
          precoMedio = estado.quantidade > 0 ? estado.custo / estado.quantidade : null;
        } else {
          precoMedio = estado.quantidade > 0 ? mediaAnterior : null;
          if (estado.quantidade > 0) lucroPrejuizo = (preco - mediaAnterior) * quantidade;
          estado.quantidade = Math.max(0, estado.quantidade - quantidade);
          estado.custo = estado.quantidade > 0 ? mediaAnterior * estado.quantidade : 0;
        }
        estados.set(chave, estado);
        return { operacao, valorTotal: quantidade * preco, precoMedio, lucroPrejuizo };
      })
      .sort((a, b) => b.operacao.dataOperacao.localeCompare(a.operacao.dataOperacao) || b.operacao.id - a.operacao.id);
  }
}
