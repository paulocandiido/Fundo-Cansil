import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { firstValueFrom } from 'rxjs';
import { Api, API_URL } from './api';
import { Sessao } from './sessao';
import { environment as dockerEnvironment } from '../../environments/environment.docker';

describe('Contrato com o backend', () => {
  let api: Api; let http: HttpTestingController; let sessao: Sessao;
  const usuario = { id: 1, nome: 'Teste', email: 'teste@example.invalid' };
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), { provide: API_URL, useValue: 'http://localhost:8080' }] });
    api = TestBed.inject(Api); http = TestBed.inject(HttpTestingController); sessao = TestBed.inject(Sessao);
  });
  afterEach(() => http.verify());
  function entrar() { sessao.definirToken('token-falso'); sessao.identificar(usuario); }

  it('faz login público e busca identidade com bearer antes de concluir', async () => {
    const promise = firstValueFrom(api.entrar({ email: usuario.email, senha: 'senha-falsa' }));
    const login = http.expectOne('http://localhost:8080/api/auth/login');
    expect(login.request.method).toBe('POST'); expect(login.request.headers.has('Authorization')).toBe(false);
    login.flush({ token: 'token-falso', tipo: 'Bearer', expiraEmSegundos: 3600 });
    const me = http.expectOne('http://localhost:8080/api/usuarios/me');
    expect(me.request.headers.get('Authorization')).toBe('Bearer token-falso');
    me.flush(usuario);
    expect(await promise).toEqual(usuario); expect(sessao.usuario()).toEqual(usuario);
  });
  it('cadastro não envia token e não abre sessão automaticamente', async () => {
    const dados = { ...usuario, cpf: '12312312312', senha: 'senha-falsa' };
    const promise = firstValueFrom(api.cadastrar(dados));
    const req = http.expectOne('http://localhost:8080/api/auth/cadastro');
    expect(req.request.headers.has('Authorization')).toBe(false); expect(req.request.body).toEqual(dados);
    req.flush(usuario, { status: 201, statusText: 'Created' });
    await promise; expect(sessao.usuario()).toBeNull();
  });
  it('401 apaga a sessão e não repete a chamada', async () => {
    entrar();
    const result = firstValueFrom(api.carteira()).catch(erro => erro);
    http.expectOne('http://localhost:8080/api/carteira').flush({}, { status: 401, statusText: 'Unauthorized' });
    expect((await result).status).toBe(401); expect(sessao.obterToken()).toBeNull(); expect(sessao.usuario()).toBeNull();
  });
  it('503 preserva a sessão para acessar outras funcionalidades', async () => {
    entrar();
    const result = firstValueFrom(api.carteira()).catch(erro => erro);
    http.expectOne('http://localhost:8080/api/carteira').flush({}, { status: 503, statusText: 'Unavailable' });
    expect((await result).status).toBe(503); expect(sessao.usuario()).toEqual(usuario);
  });
  it('bloqueia pedido protegido sem token', async () => {
    const erro = await firstValueFrom(api.carteira()).catch(erro => erro);
    expect(erro.status).toBe(401); http.expectNone('http://localhost:8080/api/carteira');
  });
  it('não mostra corpo arbitrário de erro nem credenciais', async () => {
    const result = firstValueFrom(api.entrar({ email: usuario.email, senha: 'senha-falsa' })).catch(erro => erro);
    http.expectOne('http://localhost:8080/api/auth/login').flush({ message: 'senha-falsa <script>' }, { status: 401, statusText: 'Unauthorized' });
    expect((await result).message).toBe('E-mail ou senha incorretos.'); expect(sessao.usuario()).toBeNull();
  });
  [502, 503, 504].forEach(status => {
    it(`informa claramente quando o servidor está indisponível no login (${status})`, async () => {
      const result = firstValueFrom(api.entrar({ email: usuario.email, senha: 'senha-falsa' })).catch(erro => erro);
      http.expectOne('http://localhost:8080/api/auth/login').flush({}, { status, statusText: 'Unavailable' });
      expect((await result).message).toBe('O servidor está iniciando ou temporariamente indisponível. Aguarde alguns segundos e tente novamente.');
      expect(sessao.usuario()).toBeNull();
    });
  });
  it('traduz erro de rede em mensagem sem detalhes internos', async () => {
    entrar();
    const result = firstValueFrom(api.carteira()).catch(erro => erro);
    http.expectOne('http://localhost:8080/api/carteira').error(new ProgressEvent('error'));
    expect((await result).status).toBe(0); expect(sessao.usuario()).toEqual(usuario);
  });
  it('logout remove identidade e token da memória', () => { entrar(); sessao.limpar(); expect(sessao.usuario()).toBeNull(); expect(sessao.obterToken()).toBeNull(); });
});

describe('Conexão same-origin no Docker', () => {
  it('usa /api no mesmo domínio e conserva o bearer após o login', async () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting(), { provide: API_URL, useValue: dockerEnvironment.apiUrl }] });
    const api = TestBed.inject(Api);
    const http = TestBed.inject(HttpTestingController);
    const usuario = { id: 1, nome: 'Teste', email: 'teste@example.invalid' };
    const login = firstValueFrom(api.entrar({ email: usuario.email, senha: 'senha-falsa' }));
    const request = http.expectOne('/api/auth/login');
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush({ token: 'token-falso', tipo: 'Bearer', expiraEmSegundos: 3600 });
    const identidade = http.expectOne('/api/usuarios/me');
    expect(identidade.request.headers.get('Authorization')).toBe('Bearer token-falso');
    identidade.flush(usuario);
    expect(await login).toEqual(usuario);
    const carteira = firstValueFrom(api.carteira());
    const consulta = http.expectOne('/api/carteira');
    expect(consulta.request.headers.get('Authorization')).toBe('Bearer token-falso');
    consulta.flush([]);
    expect(await carteira).toEqual([]);
    http.verify();
  });
});
