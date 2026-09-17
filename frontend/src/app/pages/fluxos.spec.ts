import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter, UrlTree } from '@angular/router';
import { registerLocaleData } from '@angular/common';
import pt from '@angular/common/locales/pt';
import { LOCALE_ID } from '@angular/core';
import { FormControl } from '@angular/forms';
import { CadastroPage } from './cadastro';
import { CarteiraPage } from './carteira';
import { LoginPage } from './login';
import { Sessao } from '../core/sessao';
import { autenticado, visitante } from '../core/auth.guard';
import { limiteSenhaUtf8 } from '../core/validadores';
import { routes } from '../app.routes';
import { vi } from 'vitest';

registerLocaleData(pt);
describe('Telas e proteção de navegação', () => {
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter(routes), { provide: LOCALE_ID, useValue: 'pt-BR' }] });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('apresenta Fundo Cansil no login e nos títulos de todas as telas', async () => {
    const fixture = TestBed.createComponent(LoginPage);
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Bem-vindo de volta ao Fundo Cansil.');
    const telas = routes.filter((route) => route.component);
    expect(telas).toHaveLength(6);
    for (const tela of telas) expect(tela.title).toMatch(/ \| Fundo Cansil$/);
  });
  function entrar() { const sessao = TestBed.inject(Sessao); sessao.definirToken('teste'); sessao.identificar({ id: 1, nome: 'Teste', email: 'teste@example.invalid' }); }
  it('redireciona visitante da carteira para o login', () => {
    const result = TestBed.runInInjectionContext(() => autenticado({} as never, {} as never));
    expect(result instanceof UrlTree).toBe(true); expect(result?.toString()).toBe('/login');
  });
  it('usuário autenticado não volta ao formulário de login', () => {
    entrar(); const result = TestBed.runInInjectionContext(() => visitante({} as never, {} as never));
    expect(result?.toString()).toBe('/carteira');
  });
  it('formulário vazio não envia cadastro', () => {
    const fixture = TestBed.createComponent(CadastroPage); fixture.componentInstance.enviar();
    expect(fixture.componentInstance.form.invalid).toBe(true); http.expectNone('http://localhost:8080/api/auth/cadastro');
  });
  it('senha multibyte respeita o limite do BCrypt', () => {
    expect(limiteSenhaUtf8(new FormControl('é'.repeat(36)))).toBeNull();
    expect(limiteSenhaUtf8(new FormControl('é'.repeat(37)))).toEqual({ bytesSenha: true });
  });
  it('cadastro envia os campos corretos e impede duplicação durante envio', async () => {
    const page = TestBed.createComponent(CadastroPage).componentInstance;
    const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    page.form.setValue({ nome: ' Teste ', email: ' teste@example.invalid ', cpf: '12312312312', senha: 'senha-falsa', confirmacao: 'senha-falsa' });
    page.enviar(); page.enviar();
    const request = http.expectOne('http://localhost:8080/api/auth/cadastro');
    expect(request.request.body.nome).toBe('Teste'); expect(request.request.body.confirmacao).toBeUndefined();
    request.flush({ id: 1, nome: 'Teste', email: 'teste@example.invalid' });
    expect(navegar).toHaveBeenCalledWith(['/login'], { queryParams: { cadastro: 'sucesso' } });
    expect(page.form.controls.senha.value).toBe('');
  });
  it('não envia cadastro com confirmação divergente', () => {
    const page = TestBed.createComponent(CadastroPage).componentInstance;
    page.form.setValue({ nome: 'Teste', email: 'teste@example.invalid', cpf: '12312312312', senha: 'senha-falsa', confirmacao: 'outra-senha' });
    page.enviar(); expect(page.form.hasError('senhasDiferentes')).toBe(true); http.expectNone('http://localhost:8080/api/auth/cadastro');
  });
  it('login inválido não chama API', () => {
    const page = TestBed.createComponent(LoginPage).componentInstance; page.enviar();
    http.expectNone('http://localhost:8080/api/auth/login');
  });
  it('carteira vazia aparece sem posições fictícias', async () => {
    entrar(); const fixture = TestBed.createComponent(CarteiraPage); fixture.detectChanges();
    http.expectOne('http://localhost:8080/api/carteira').flush([]);
    http.expectOne('http://localhost:8080/api/transacoes').flush([]);
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Sua carteira ainda está vazia');
    expect(fixture.nativeElement.querySelectorAll('tbody tr').length).toBe(0);
  });
  it('formata valores em português preservando até seis casas', async () => {
    entrar(); const fixture = TestBed.createComponent(CarteiraPage); fixture.detectChanges();
    http.expectOne('http://localhost:8080/api/carteira').flush([{ simbolo: 'PETR4', quantidade: 1.234567, precoMedio: 20.5, custoTotal: 25.31 }]);
    http.expectOne('http://localhost:8080/api/transacoes').flush([]);
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('PETR4'); expect(fixture.nativeElement.textContent).toContain('1,234567'); expect(fixture.nativeElement.textContent).toContain('20,50');
  });
  it('sessão expirada retorna ao login', () => {
    entrar(); const navegar = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const fixture = TestBed.createComponent(CarteiraPage); fixture.detectChanges();
    http.expectOne('http://localhost:8080/api/transacoes').flush([]);
    http.expectOne('http://localhost:8080/api/carteira').flush({}, { status: 401, statusText: 'Unauthorized' });
    expect(navegar).toHaveBeenCalledWith(['/login'], { queryParams: { sessao: 'expirada' } });
  });
});
