import { LOCALE_ID } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import pt from '@angular/common/locales/pt';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { CarteiraPage } from './carteira';
import { Transacao } from '../core/modelos';

registerLocaleData(pt);

describe('Painel profissional da carteira', () => {
  const corretora = { id: 1, codigo: 'XP', nome: 'XP Investimentos', ativa: true, legada: false, cnpj: '19131243000197', mercado: 'BR' as const, verificadoEm: '2026-09-06T12:00:00Z' };
  const operacoes: Transacao[] = [
    { id: 1, simbolo: 'PETR4', corretora, tipo: 'COMPRA', quantidade: 10, valorUnitario: 20, dataOperacao: '2026-09-01T10:00:00', moeda: 'BRL' },
    { id: 2, simbolo: 'PETR4', corretora, tipo: 'COMPRA', quantidade: 10, valorUnitario: 30, dataOperacao: '2026-09-02T10:00:00', moeda: 'BRL' },
    { id: 3, simbolo: 'PETR4', corretora, tipo: 'VENDA', quantidade: 5, valorUnitario: 40, dataOperacao: '2026-09-03T10:00:00', moeda: 'BRL' },
    { id: 4, simbolo: 'AAPL', corretora, tipo: 'COMPRA', quantidade: 2, valorUnitario: 100, dataOperacao: '2026-09-04T10:00:00', moeda: 'USD' },
  ];

  beforeEach(() => TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([]), { provide: LOCALE_ID, useValue: 'pt-BR' }] }));

  function pagina() {
    const page = TestBed.createComponent(CarteiraPage).componentInstance;
    page.transacoes.set(operacoes);
    return page;
  }

  it('mantém o preço médio após a venda e calcula o resultado realizado', () => {
    const page = pagina();
    const venda = page.extrato().find(item => item.operacao.id === 3)!;
    expect(venda.precoMedio).toBe(25);
    expect(venda.valorTotal).toBe(200);
    expect(venda.lucroPrejuizo).toBe(75);
    expect(page.extrato().map(item => item.operacao.id)).toEqual([4, 3, 2, 1]);
  });

  it('combina filtros de tipo e ativo', () => {
    const page = pagina();
    page.selecionarTipo('COMPRA'); page.alterarAtivo('PETR4'); page.filtrarHistorico();
    expect(page.extratoFiltrado().map(item => item.operacao.id)).toEqual([2, 1]);
    page.selecionarTipo('VENDA');
    expect(page.extratoFiltrado().map(item => item.operacao.id)).toEqual([3]);
  });

  it('consulta operação por ID e informa entradas vazias ou inexistentes', () => {
    const page = pagina();
    page.consultarPorId(); expect(page.mensagemConsulta()).toBe('Informe um identificador.');
    page.identificador.set('999'); page.consultarPorId(); expect(page.mensagemConsulta()).toBe('Operação não encontrada');
    page.identificador.set('3'); page.consultarPorId();
    expect(page.operacaoConsultada()?.operacao.tipo).toBe('VENDA'); expect(page.operacaoConsultada()?.valorTotal).toBe(200);
  });

  it('não soma reais e dólares nos indicadores', () => {
    const page = pagina();
    page.posicoes.set([
      { simbolo: 'PETR4', quantidade: 15, precoMedio: 25, custoTotal: 375, moeda: 'BRL' },
      { simbolo: 'AAPL', quantidade: 2, precoMedio: 100, custoTotal: 200, moeda: 'USD' },
    ]);
    expect(page.resumos()).toEqual([
      { moeda: 'BRL', precoMedio: 25, custoTotal: 375, valorAtual: null },
      { moeda: 'USD', precoMedio: 100, custoTotal: 200, valorAtual: null },
    ]);
    page.cotacoes.set(new Map([
      ['PETR4', { simbolo: 'PETR4', quantidade: 15, precoMedio: 25, custoTotal: 375, cotacaoAtual: 30, valorAtual: 450, lucroPrejuizo: 75, lucroPrejuizoPercentual: 20, fonte: 'BRAPI', moeda: 'BRL' }],
      ['AAPL', { simbolo: 'AAPL', quantidade: 2, precoMedio: 100, custoTotal: 200, cotacaoAtual: 110, valorAtual: 220, lucroPrejuizo: 20, lucroPrejuizoPercentual: 10, fonte: 'ALPHA_VANTAGE', moeda: 'USD' }],
    ]));
    expect(page.resumos().map(item => item.valorAtual)).toEqual([450, 220]);
  });

  it('preserva o custo recebido sem reconstruí-lo pela média arredondada', () => {
    const page = TestBed.createComponent(CarteiraPage).componentInstance;
    page.posicoes.set([{ simbolo: 'TESTE', quantidade: 30000, precoMedio: 1.666667, custoTotal: 50000, moeda: 'BRL' }]);
    expect(page.resumos()[0].custoTotal).toBe(50000);
    expect(page.resumos()[0].precoMedio).toBeCloseTo(50000 / 30000, 10);
  });
  it('calcula a média aprovada pela quantidade vezes o preço divididos pela quantidade', () => {
    const page = pagina();
    page.posicoes.set([{ simbolo: 'TESTE1', quantidade: 20, precoMedio: 316.22, custoTotal: 6324.4, moeda: 'BRL' }]);
    expect(page.resumos()[0].custoTotal).toBeCloseTo(6324.4, 8);
    expect(page.resumos()[0].precoMedio).toBeCloseTo(316.22, 8);
    page.posicoes.set([{ simbolo: 'TESTE2', quantidade: 5, precoMedio: 5, custoTotal: 25, moeda: 'BRL' }]);
    expect(page.resumos()[0].custoTotal).toBe(25);
    expect(page.resumos()[0].precoMedio).toBe(5);
  });
});
