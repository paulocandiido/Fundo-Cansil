import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter, Router } from '@angular/router';
import { RouterTestingHarness } from '@angular/router/testing';
import { FormControl } from '@angular/forms';
import { vi } from 'vitest';
import { OperacoesPage } from './operacoes';
import { CotacoesPage } from './cotacoes';
import { Sessao } from '../core/sessao';
import { DataLocalPipe, dataLocalValida, dataParaApi, decimalParaApi, decimalPositivo, simboloValido } from '../core/operacao-validadores';
import { routes } from '../app.routes';
import { autenticado } from '../core/auth.guard';
import { OperacaoEmEnvio } from '../core/operacao-em-envio';

describe('Operações e consultas de investimentos', () => {
  let http: HttpTestingController;
  const base = 'http://localhost:8080/api';
  const corretora = { id: 1, codigo: 'XP', nome: 'XP Investimentos', ativa: true, legada: false, cnpj: '19131243000197', mercado: 'BR' as const, verificadoEm: '2026-09-06T12:00:00Z' };
  const dados = { mercado: 'BR' as const, simbolo: ' petr4 ', corretoraId: 1, tipo: 'COMPRA' as const, quantidade: '0,000001', valorUnitario: '9999999999999,123456' };
  const registrada = { id: 15, simbolo: 'PETR4', corretora, tipo: 'COMPRA', quantidade: 1, valorUnitario: 20, dataOperacao: '2026-09-03T09:30:00' };
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter(routes)] });
    http = TestBed.inject(HttpTestingController);
    const sessao = TestBed.inject(Sessao); sessao.definirToken('token-falso'); sessao.identificar({ id: 1, nome: 'Teste', email: 'teste@example.invalid' });
  });
  afterEach(() => http.verify());
  function formulario() { const page = TestBed.createComponent(OperacoesPage).componentInstance; page.corretoras.set([corretora]); page.posicoesProprias.set([{ simbolo: 'PETR4', corretora, quantidade: 10, precoMedio: 20, custoTotal: 200 }]); page.form.setValue(dados); return page; }

  it('envia decimais exatos sem permitir data informada pelo usuário, com apenas um POST', () => {
    const page = formulario(); page.enviar(); page.enviar();
    const req = http.expectOne(base + '/transacoes');
    expect(req.request.method).toBe('POST'); expect(req.request.headers.get('Authorization')).toBe('Bearer token-falso');
    expect(req.request.body).toEqual({ mercado: 'BR', simbolo: 'PETR4', corretoraId: 1, tipo: 'COMPRA', quantidade: '0.000001', valorUnitario: '9999999999999.123456' });
    expect(req.request.body.moeda).toBeUndefined();
    expect(req.request.body.dataOperacao).toBeUndefined();
    expect(req.request.body.usuarioId).toBeUndefined(); expect(page.form.disabled).toBe(true);
    req.flush(registrada, { status: 201, statusText: 'Created' });
    expect(page.sucesso()).toContain('#15'); expect(page.form.controls.quantidade.value).toBe('');
    http.expectOne(base + '/transacoes').flush([registrada]); expect(page.transacoes()).toHaveLength(1);
    http.expectOne(base + '/corretoras').flush([corretora]); http.expectOne(base + '/carteira/corretoras').flush([]);
  });
  it('422 mantém os campos para correção e não limpa a sessão', () => {
    const page = formulario(); page.form.controls.tipo.setValue('VENDA'); page.enviar();
    http.expectOne(base + '/transacoes').flush({ message: 'Venda superior ao saldo disponível' }, { status: 422, statusText: 'Unprocessable Entity' });
    expect(page.erro()).toContain('saldo'); expect(page.form.controls.tipo.value).toBe('VENDA'); expect(page.form.enabled).toBe(true);
    expect(page.incerto()).toBe(false); expect(TestBed.inject(Sessao).usuario()).not.toBeNull();
  });
  it('não reenvia após erro de rede até atualização e confirmação do histórico', () => {
    const page = formulario(); page.enviar();
    http.expectOne(base + '/transacoes').error(new ProgressEvent('error'));
    expect(page.incerto()).toBe(true); page.enviar(); page.liberarReenvio(); expect(page.incerto()).toBe(true);
    http.expectNone(base + '/transacoes');
    page.carregarHistorico(); http.expectOne(base + '/transacoes').flush([]);
    expect(page.historicoConferivel()).toBe(true); page.liberarReenvio(); expect(page.incerto()).toBe(false);
  });
  it('não usa histórico iniciado antes da falha como confirmação para reenvio', () => {
    const page = formulario(); page.carregarHistorico(); const antigo = http.expectOne(base + '/transacoes');
    page.enviar(); http.expectOne(base + '/transacoes').error(new ProgressEvent('error'));
    antigo.flush([]); page.liberarReenvio(); expect(page.incerto()).toBe(true); expect(page.historicoConferivel()).toBe(false);
  });
  it('falha ao atualizar histórico não transforma compra confirmada em falha de gravação', () => {
    const page = formulario(); page.enviar(); http.expectOne(base + '/transacoes').flush(registrada);
    http.expectOne(base + '/transacoes').flush({}, { status: 500, statusText: 'Error' });
    http.expectOne(base + '/corretoras').flush([corretora]); http.expectOne(base + '/carteira/corretoras').flush([]);
    expect(page.sucesso()).toContain('registrada'); expect(page.erro()).toBe(''); expect(page.erroHistorico()).not.toBe(''); expect(page.incerto()).toBe(false);
  });
  it('401 em operação encaminha ao login e remove a sessão', () => {
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const page = formulario(); page.enviar(); http.expectOne(base + '/transacoes').flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(navegar).toHaveBeenCalledWith(['/login'], { queryParams: { sessao: 'expirada' } }); expect(TestBed.inject(Sessao).usuario()).toBeNull();
  });
  it('histórico mostra as operações por data e id decrescentes', () => {
    const page = formulario(); page.carregarHistorico();
    http.expectOne(base + '/transacoes').flush([{ ...registrada, id: 1 }, { ...registrada, id: 2 }, { ...registrada, id: 3, dataOperacao: '2026-09-04T10:00:00' }]);
    expect(page.transacoes().map(item => item.id)).toEqual([3, 2, 1]);
  });
  it('aguarda o POST terminar antes de encaminhar ao login por 401 do histórico', () => {
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const page = formulario(); page.carregarHistorico();
    const historico = http.expectOne(base + '/transacoes');
    page.enviar(); const post = http.expectOne(base + '/transacoes');
    historico.flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(navegar).not.toHaveBeenCalled(); expect(post.cancelled).toBe(false);
    post.flush({}, { status: 422, statusText: 'Unprocessable Entity' });
    expect(page.enviando()).toBe(false);
    expect(navegar).toHaveBeenCalledExactlyOnceWith(['/login'], { queryParams: { sessao: 'expirada' } });
  });
  it('401 de histórico antigo não deixa a tela aberta sem sessão', () => {
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const page = formulario(); page.carregarHistorico(); const antigo = http.expectOne(base + '/transacoes');
    page.carregarHistorico(); http.expectOne(base + '/transacoes').flush([registrada]);
    antigo.flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(page.transacoes()).toHaveLength(1);
    expect(TestBed.inject(Sessao).usuario()).toBeNull();
    expect(navegar).toHaveBeenCalledWith(['/login'], { queryParams: { sessao: 'expirada' } });
  });
  it('não redireciona por resposta antiga quando já existe outro token válido', () => {
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const page = formulario(); page.carregarHistorico(); const antigo = http.expectOne(base + '/transacoes');
    TestBed.inject(Sessao).definirToken('outro-token-falso');
    antigo.flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(TestBed.inject(Sessao).obterToken()).toBe('outro-token-falso');
    expect(navegar).not.toHaveBeenCalled();
  });
  it('conclui compra após expiração paralela e redireciona sem buscar histórico sem token', () => {
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const page = formulario(); page.carregarHistorico(); const historico = http.expectOne(base + '/transacoes');
    page.enviar(); const post = http.expectOne(base + '/transacoes');
    historico.flush({}, { status: 401, statusText: 'Unauthorized' });
    post.flush(registrada, { status: 201, statusText: 'Created' });
    expect(page.sucesso()).toContain('#15'); expect(page.enviando()).toBe(false);
    expect(navegar).toHaveBeenCalledTimes(1); http.expectNone(base + '/transacoes');
  });
  it('roteador mantém operação pendente e abre login após a resposta final', async () => {
    const harness = await RouterTestingHarness.create();
    const page = await harness.navigateByUrl('/operacoes', OperacoesPage);
    http.expectOne(base + '/corretoras').flush([corretora]);http.expectOne(base + '/carteira/corretoras').flush([]);
    const historico = http.expectOne(base + '/transacoes');
    page.form.setValue(dados); page.enviar(); const post = http.expectOne(base + '/transacoes');
    historico.flush({}, { status: 401, statusText: 'Unauthorized' });
    await harness.fixture.whenStable();
    expect(TestBed.inject(Router).url).toBe('/operacoes'); expect(post.cancelled).toBe(false);
    post.flush({}, { status: 422, statusText: 'Unprocessable Entity' });
    await harness.fixture.whenStable();
    expect(TestBed.inject(Router).url).toBe('/login?sessao=expirada');
    expect(TestBed.inject(OperacaoEmEnvio).ativa()).toBe(false);
  });
  it('trata parâmetros de venda como atalho sem autoridade e não preenche o preço', async () => {
    const harness = await RouterTestingHarness.create();
    const page = await harness.navigateByUrl('/operacoes?tipo=VENDA&simbolo=PETR4&corretoraId=1', OperacoesPage);
    http.expectOne(base + '/transacoes').flush([]);
    http.expectOne(base + '/corretoras').flush([corretora]);
    expect(page.form.controls.simbolo.value).toBe(''); expect(page.form.controls.corretoraId.value).toBe(0);
    http.expectOne(base + '/ativos?busca=PETR4&pagina=0&tamanho=20').flush({ itens: [{ simbolo: 'PETR4' }], pagina: 0, tamanho: 20, total: 1, atualizadoEm: '2026-09-06T12:00:00Z', desatualizado: false });
    expect(page.form.controls.simbolo.value).toBe('PETR4'); expect(page.form.controls.corretoraId.value).toBe(0);
    http.expectOne(base + '/carteira/corretoras').flush([{ simbolo: 'PETR4', corretora, quantidade: 5, precoMedio: 20, custoTotal: 100 }]);
    expect(page.form.controls.corretoraId.value).toBe(1); expect(page.form.controls.valorUnitario.value).toBe(''); expect(page.form.controls.quantidade.value).toBe('');
  });
  it('dados inválidos não fazem POST', () => {
    const page = formulario(); page.form.controls.quantidade.setValue('0'); page.enviar();
    expect(page.form.invalid).toBe(true); http.expectNone(base + '/transacoes');
  });
  it('define a moeda pelo mercado sem oferecer moeda manual', () => {
    const page = formulario();
    expect(page.moeda()).toBe('BRL');
    page.form.controls.mercado.setValue('USA');
    expect(page.moeda()).toBe('USD');
    page.form.controls.simbolo.setValue('AAPL'); page.enviar();
    const req=http.expectOne(base+'/transacoes');
    expect(req.request.body.mercado).toBe('USA');expect(req.request.body.moeda).toBeUndefined();
    req.flush({}, { status: 422, statusText: 'Unprocessable Entity' });
  });
  it.each(['COMPRA', 'VENDA'] as const)('preserva a corretora ao buscar e selecionar um ativo para %s', async tipo => {
    const fixture = TestBed.createComponent(OperacoesPage);
    fixture.detectChanges();
    http.expectOne(base + '/transacoes').flush([]);
    http.expectOne(base + '/corretoras').flush([corretora]);
    http.expectOne(base + '/carteira/corretoras').flush([{ simbolo: 'PETR4', corretora, quantidade: 10, precoMedio: 20, custoTotal: 200 }]);
    const page = fixture.componentInstance;
    page.selecionarTipo(tipo);
    fixture.detectChanges();
    const selectCorretora: HTMLSelectElement = fixture.nativeElement.querySelector('#corretora-operacao');
    selectCorretora.selectedIndex = 1;
    selectCorretora.dispatchEvent(new Event('change'));
    const busca: HTMLInputElement = fixture.nativeElement.querySelector('#busca-ativo');
    busca.value = 'PETR4';
    busca.dispatchEvent(new Event('input'));
    expect(page.form.controls.corretoraId.value).toBe(1);
    page.buscarAtivos();
    http.expectOne(base + '/ativos?busca=PETR4&pagina=0&tamanho=20').flush({ itens: [{ simbolo: 'PETR4' }], pagina: 0, tamanho: 20, total: 1, desatualizado: false });
    fixture.detectChanges();
    const selectAtivo: HTMLSelectElement = fixture.nativeElement.querySelector('#simbolo-operacao');
    selectAtivo.value = 'PETR4';
    selectAtivo.dispatchEvent(new Event('change'));
    http.expectOne(base + '/cotacoes/PETR4').flush({ simbolo: 'PETR4', preco: 20, moeda: 'BRL' });
    await fixture.whenStable();
    expect(page.form.controls.corretoraId.value).toBe(1);
    expect(selectCorretora.selectedOptions[0].textContent).toContain('XP Investimentos');
  });
  it('limpa a corretora ao selecionar para venda um ativo sem posição nela', () => {
    const page = formulario();
    page.form.controls.tipo.setValue('VENDA');
    page.selecionarAtivo('VALE3');
    http.expectOne(base + '/cotacoes/VALE3').flush({ simbolo: 'VALE3', preco: 60, moeda: 'BRL' });
    expect(page.form.controls.corretoraId.value).toBe(0);
  });
  it('preenche o preço unitário com a cotação ao selecionar o ativo', () => {
    const page = formulario();
    page.form.controls.valorUnitario.setValue('99');
    page.selecionarAtivo('PETR4');
    expect(page.form.controls.valorUnitario.value).toBe('');
    expect(page.carregandoCotacao()).toBe(true);
    http.expectOne(base + '/cotacoes/PETR4').flush({ simbolo: 'PETR4', nome: 'Petrobras', bolsa: 'B3', preco: 20.5, fonte: 'BRAPI', moeda: 'BRL' });
    expect(page.form.controls.valorUnitario.value).toBe('20,5');
    expect(page.carregandoCotacao()).toBe(false);
    expect(page.erroCotacao()).toBe('');
  });
  it('não reaproveita preço anterior quando a cotação do novo ativo falha', () => {
    const page = formulario();
    page.selecionarAtivo('VALE3');
    http.expectOne(base + '/cotacoes/VALE3').flush({}, { status: 503, statusText: 'Unavailable' });
    expect(page.form.controls.valorUnitario.value).toBe('');
    expect(page.erroCotacao()).toContain('manualmente');
    expect(TestBed.inject(Sessao).usuario()).not.toBeNull();
  });
  it('rejeita valores negativos, zero, notação científica, milhares e precisão excedida', () => {
    for (const valor of ['0', '0,000000', '-1', '1e3', '1.000,50', '10000000000000', '1.0000001', '', 'NaN']) expect(decimalPositivo(new FormControl(valor))).not.toBeNull();
    for (const valor of ['1', '0.000001', '9999999999999,123456']) expect(decimalPositivo(new FormControl(valor))).toBeNull();
    expect(decimalParaApi(' 0,000001 ')).toBe('0.000001');
  });
  it('valida calendário sem depender do fuso e preserva segundos', () => {
    expect(dataLocalValida(new FormControl('2024-02-29T23:59:59'))).toBeNull();
    for (const data of ['2025-02-29T10:00', '2026-04-31T10:00', '2026-09-03T24:00', '2026-09-03T10:00:00Z', '']) expect(dataLocalValida(new FormControl(data))).not.toBeNull();
    expect(dataParaApi('2026-09-03T10:00')).toBe('2026-09-03T10:00:00');
    expect(new DataLocalPipe().transform('2026-09-03T10:00:45.123')).toBe('03/09/2026 10:00:45');
  });
  it('rejeita símbolos fora do contrato', () => {
    expect(simboloValido(new FormControl('petr4'))).toBeNull();
    expect(simboloValido(new FormControl('PETR4/../'))).not.toBeNull();
  });
  it('entrar na tela de cotações só lê histórico, sem consumir cotação', () => {
    const page = TestBed.createComponent(CotacoesPage).componentInstance; page.ngOnInit();
    http.expectOne(base + '/cotacoes/historico').flush([]); expect(page.resultado()).toBeNull();
  });
  it('busca normalizada mantém fonte dinâmica e atualiza histórico somente no sucesso', () => {
    const page = TestBed.createComponent(CotacoesPage).componentInstance;
    page.form.controls.simbolo.setValue(' petr4 '); page.buscar(); page.buscar();
    http.expectOne(base + '/cotacoes/PETR4').flush({ simbolo: 'PETR4', nome: null, bolsa: null, preco: 20.5, fonte: 'NOVA_FONTE' });
    expect(page.resultado()?.fonte).toBe('NOVA_FONTE'); http.expectOne(base + '/cotacoes/historico').flush([]);
  });
  it('falha de cotação não mostra o resultado anterior nem elimina a sessão', () => {
    const page = TestBed.createComponent(CotacoesPage).componentInstance;
    page.resultado.set({ simbolo: 'VALE3', nome: 'Vale', bolsa: 'B3', preco: 60, fonte: 'BRAPI' });
    page.form.controls.simbolo.setValue('PETR4'); page.buscar();
    http.expectOne(base + '/cotacoes/PETR4').flush({}, { status: 503, statusText: 'Unavailable' });
    expect(page.resultado()).toBeNull(); expect(page.erro()).toContain('indisponíveis'); expect(TestBed.inject(Sessao).usuario()).not.toBeNull();
    http.expectNone(base + '/cotacoes/historico');
  });
  it('todas as novas rotas exigem autenticação', () => {
    for (const path of ['operacoes', 'cotacoes']) expect(routes.find(route => route.path === path)?.canActivate).toContain(autenticado);
    expect(routes.find(route => route.path === 'avaliacao')).toBeUndefined();
  });
  it('protege a navegação durante o registro e libera após a resposta', () => {
    const page = formulario(); page.enviar();
    const guard = routes.find(route => route.path === 'operacoes')!.canDeactivate![0] as (page: OperacoesPage) => boolean;
    expect(guard(page)).toBe(false); expect(TestBed.inject(OperacaoEmEnvio).ativa()).toBe(true);
    http.expectOne(base + '/transacoes').flush({}, { status: 422, statusText: 'Unprocessable Entity' });
    expect(guard(page)).toBe(true); expect(TestBed.inject(OperacaoEmEnvio).ativa()).toBe(false);
  });
  it('renderiza formulário e histórico vazio', async () => {
    const fixture = TestBed.createComponent(OperacoesPage); fixture.detectChanges();
    http.expectOne(base + '/transacoes').flush([]);http.expectOne(base + '/corretoras').flush([corretora]);http.expectOne(base + '/carteira/corretoras').flush([]); await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('#tipo-operacao')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('#mercado-operacao')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('#moeda-operacao')).toBeNull();
    expect(fixture.nativeElement.querySelector('#data-operacao')).toBeNull();
    expect(fixture.nativeElement.querySelector('.security-note')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Nenhuma operação registrada');
  });
  it('renderiza cotação com metadados nulos e fonte alternativa', async () => {
    const fixture = TestBed.createComponent(CotacoesPage); fixture.detectChanges();
    http.expectOne(base + '/cotacoes/historico').flush([]);
    fixture.componentInstance.form.controls.simbolo.setValue('PETR4'); fixture.componentInstance.buscar();
    http.expectOne(base + '/cotacoes/PETR4').flush({ simbolo: 'PETR4', nome: null, bolsa: null, preco: 25, fonte: 'OUTRA_FONTE' });
    http.expectOne(base + '/cotacoes/historico').flush([]); await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('OUTRA_FONTE');
    expect(fixture.nativeElement.textContent).toContain('Bolsa não informada');
  });
});
