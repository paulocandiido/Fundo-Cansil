import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Sessao } from '../core/sessao';
import { cnpjValido } from '../core/cnpj';
import { CorretorasPage } from './corretoras';
import { OperacoesPage } from './operacoes';

describe('Cadastro de corretoras por CNPJ', () => {
  let http: HttpTestingController;
  const base = 'http://localhost:8080/api';
  const consulta = { cnpj: '19131243000197', razaoSocial: 'RAZÃO SOCIAL DA FONTE', situacaoCadastral: 'ATIVA', ativa: true, consultadoEm: '2026-09-06T12:00:00Z' };
  const corretora = { id: 10, codigo: 'uuid', nome: consulta.razaoSocial, ativa: true, legada: false, cnpj: consulta.cnpj, mercado: 'BR' as const, verificadoEm: consulta.consultadoEm };
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])] });
    http = TestBed.inject(HttpTestingController);
    TestBed.inject(Sessao).definirToken('token-falso');
  });
  afterEach(() => http.verify());
  function tela() {
    const fixture = TestBed.createComponent(CorretorasPage); fixture.detectChanges();
    http.expectOne(base + '/corretoras').flush([]);
    return fixture;
  }
  function consultar(page: CorretorasPage) {
    page.form.controls.cnpj.setValue('19.131.243/0001-97'); page.consultar();
    http.expectOne(base + '/corretoras/cnpj/19131243000197').flush(consulta);
  }
  it('confere dígitos verificadores numéricos e alfanuméricos', () => {
    expect(cnpjValido('19.131.243/0001-97')).toBe(true); expect(cnpjValido('12.ABC.345/01DE-35')).toBe(true);
    for (const cnpj of ['00000000000000', '19131243000198', '12ABC34501DE34', '19 131243000197']) expect(cnpjValido(cnpj)).toBe(false);
  });
  it('consulta explicitamente e salva somente CNPJ, nunca o nome digitado', () => {
    const fixture = tela(), page = fixture.componentInstance;
    consultar(page); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#razao-social').readOnly).toBe(true);
    page.salvar(); page.salvar();
    const req = http.expectOne(base + '/corretoras');
    expect(req.request.headers.get('Authorization')).toBe('Bearer token-falso');
    expect(req.request.body).toEqual({ cnpj: consulta.cnpj });
    expect(page.form.disabled).toBe(true); req.flush(corretora);
    expect(page.corretoras()).toHaveLength(1); expect(page.sucesso()).toContain(corretora.nome); expect(page.enviando()).toBe(false);
  });
  it('descarta a resposta antiga quando o CNPJ muda', () => {
    const page = tela().componentInstance;
    page.form.controls.cnpj.setValue(consulta.cnpj); page.consultar();
    const antiga = http.expectOne(base + '/corretoras/cnpj/' + consulta.cnpj);
    page.form.controls.cnpj.setValue('00000000000191'); antiga.flush(consulta);
    expect(page.consulta()).toBeNull(); page.salvar(); http.expectNone(base + '/corretoras');
  });
  it('falha cadastral mantém formulário sem nome confirmado', () => {
    const page = tela().componentInstance;
    page.form.controls.cnpj.setValue(consulta.cnpj); page.consultar();
    http.expectOne(base + '/corretoras/cnpj/' + consulta.cnpj).flush({}, { status: 503, statusText: 'Unavailable' });
    expect(page.consulta()).toBeNull(); expect(page.erro()).toContain('cadastral'); page.salvar(); http.expectNone(base + '/corretoras');
  });
  it('cadastro não exige nem oferece mercado fixo', () => {
    const fixture = tela(), page = fixture.componentInstance; consultar(page); fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#mercado-corretora')).toBeNull();
    expect(fixture.nativeElement.textContent).toContain('qualquer mercado');
    page.salvar(); const req = http.expectOne(base + '/corretoras');
    expect(req.request.body.mercado).toBeUndefined(); req.flush(corretora);
  });
  it('regularização envia id explícito sem vinculação de mercado', () => {
    const page = tela().componentInstance;
    page.regularizar({ ...corretora, cnpj: null, legada: true, ativa: false, verificadoEm: null });
    consultar(page); page.salvar();
    const req = http.expectOne(base + '/corretoras');
    expect(req.request.body).toEqual({ cnpj: consulta.cnpj, corretoraLegadaId: 10 }); req.flush(corretora);
    expect(page.legada()).toBeNull();
  });
  it('resultado incerto exige conferir lista e não repete o POST', () => {
    const page = tela().componentInstance; consultar(page); page.salvar();
    http.expectOne(base + '/corretoras').error(new ProgressEvent('error'));
    expect(page.incerto()).toBe(true); page.salvar(); http.expectNone(base + '/corretoras');
    page.carregar(); http.expectOne(base + '/corretoras').flush([corretora]);
    expect(page.incerto()).toBe(false); expect(page.corretoras()).toHaveLength(1);
  });
  it('envia operação BR com corretora anteriormente USA', () => {
    const page = TestBed.createComponent(OperacoesPage).componentInstance;
    page.corretoras.set([{ ...corretora, mercado: 'USA' }]);
    page.form.setValue({ mercado: 'BR', simbolo: 'PETR4', corretoraId: 10, tipo: 'COMPRA', quantidade: '1', valorUnitario: '20' });
    page.enviar();
    http.expectOne(base + '/transacoes').flush({id:1,simbolo:'PETR4',corretora,tipo:'COMPRA'});
    http.expectOne(base + '/transacoes').flush([]);
    http.expectOne(base + '/corretoras').flush([corretora]);
    http.expectOne(base + '/carteira/corretoras').flush([]);
    expect(page.erro()).toBe('');
  });
  it('remove só após confirmação e evita DELETE duplicado', () => {
    const fixture=tela(), page=fixture.componentInstance;
    page.corretoras.set([corretora]); page.solicitarRemocao(corretora);
    http.expectNone(base+'/corretoras/10'); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('excluídas permanentemente');
    page.cancelarRemocao(); page.confirmarRemocao(); http.expectNone(base+'/corretoras/10');
    page.solicitarRemocao(corretora); page.confirmarRemocao(); page.confirmarRemocao();
    const req=http.expectOne(base+'/corretoras/10'); expect(req.request.method).toBe('DELETE');
    expect(page.enviando()).toBe(true); req.flush(null,{status:204,statusText:'No Content'});
    expect(page.corretoras()).toHaveLength(0); expect(page.enviando()).toBe(false);
    expect(page.sucesso()).toContain('operações relacionadas excluídas');
  });
  it('remoção incerta exige atualização antes de nova tentativa', () => {
    const page=tela().componentInstance; page.corretoras.set([corretora]);
    page.solicitarRemocao(corretora); page.confirmarRemocao();
    http.expectOne(base+'/corretoras/10').error(new ProgressEvent('error'));
    expect(page.incerto()).toBe(true); expect(page.corretoras()).toHaveLength(1);
    page.confirmarRemocao(); http.expectNone(base+'/corretoras/10');
    page.carregar(); http.expectOne(base+'/corretoras').flush([]);
    expect(page.incerto()).toBe(false); expect(page.removendo()).toBeNull();
  });
  it('avisa que excluir corretora também exclui operações e posições', () => {
    const fixture=tela(), page=fixture.componentInstance;
    page.corretoras.set([corretora]); page.solicitarRemocao(corretora); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('compras, vendas e posições');
    expect(fixture.nativeElement.textContent).not.toContain('histórico e os saldos serão preservados');
  });
});
