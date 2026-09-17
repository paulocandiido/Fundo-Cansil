// Executar com Node 24+: node --test docs/frontend/api-client.test.mjs
import test from 'node:test';
import assert from 'node:assert/strict';
import { ApiClient, ApiError } from './api-client.ts';

const json = (body, status = 200) => new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
const loginResponse = () => json({ token: 'token-falso-de-teste', tipo: 'Bearer', expiraEmSegundos: 3600 });
const credentials = { email: 'teste@example.invalid', senha: 'senha-falsa' };

test('remoção envia DELETE autenticado e aceita resposta 204 sem JSON', async () => {
  const calls=[];
  const api=new ApiClient(undefined,async(url,init)=>{
    calls.push({url,init}); return calls.length===1?loginResponse():new Response(null,{status:204});
  });
  await api.login(credentials);
  assert.equal(await api.removerCorretora(12),undefined);
  assert.equal(calls[1].url,'http://localhost:8080/api/corretoras/12');
  assert.equal(calls[1].init.method,'DELETE');
  assert.equal(calls[1].init.headers.Authorization,'Bearer token-falso-de-teste');
});

test('consulta CNPJ e cadastro de corretora usam bearer e não enviam razão social livre', async () => {
  const calls = [];
  const api = new ApiClient(undefined, async (url, init) => { calls.push({url,init}); return calls.length === 1 ? loginResponse() : json({}); });
  await api.login(credentials);
  await api.consultarCnpj('19.131.243/0001-97');
  await api.cadastrarCorretora({ cnpj: '19131243000197', mercado: 'BR' });
  assert.equal(calls[1].url, 'http://localhost:8080/api/corretoras/cnpj/19131243000197');
  assert.equal(calls[1].init.headers.Authorization, 'Bearer token-falso-de-teste');
  assert.equal(calls[2].init.method, 'POST');
  assert.deepEqual(JSON.parse(calls[2].init.body), { cnpj: '19131243000197', mercado: 'BR' });
});

test('login não envia bearer e consultas autenticadas enviam o token em memória', async () => {
  const calls = [];
  const api = new ApiClient('http://localhost:8080/', async (url, init) => {
    calls.push({ url, init });
    return calls.length === 1 ? loginResponse() : json([]);
  });
  await api.login(credentials);
  await api.carteira();
  assert.equal(calls[0].init.headers.Authorization, undefined);
  assert.equal(calls[1].init.headers.Authorization, 'Bearer token-falso-de-teste');
  assert.equal(calls[1].init.credentials, 'omit');
  assert.equal(calls[1].url, 'http://localhost:8080/api/carteira');
  api.logout();
  await assert.rejects(api.me(), error => error instanceof ApiError && error.status === 401);
  assert.equal(calls.length, 2);
});

test('401 remove sessão local', async () => {
  let calls = 0;
  const api = new ApiClient(undefined, async () => ++calls === 1 ? loginResponse() : json({ message: 'Não autenticado' }, 401));
  await api.login(credentials);
  await assert.rejects(api.me(), error => error.status === 401);
  await assert.rejects(api.carteira(), error => error.status === 401);
  assert.equal(calls, 2);
});

test('503 preserva sessão e carteira continua acessível', async () => {
  let calls = 0;
  const api = new ApiClient(undefined, async () => {
    calls++;
    if (calls === 1) return loginResponse();
    if (calls === 2) return json({ message: 'Cotação indisponível' }, 503);
    return json([]);
  });
  await api.login(credentials);
  await assert.rejects(api.avaliacao(), error => error.status === 503);
  assert.deepEqual(await api.carteira(), []);
});

test('falha de rede não repete POST e preserva decimais em string', async () => {
  let calls = 0;
  const operation = { mercado: 'BR', simbolo: 'PETR4', corretoraId: 1, tipo: 'COMPRA', quantidade: '10', valorUnitario: '20.50' };
  const api = new ApiClient(undefined, async (url, init) => {
    if (++calls === 1) return loginResponse();
    assert.equal(init.method, 'POST');
    assert.deepEqual(JSON.parse(init.body), operation);
    throw new TypeError('Falha de rede');
  });
  await api.login(credentials);
  await assert.rejects(api.registrarTransacao(operation), error => error.status === 0);
  assert.equal(calls, 2);
});

test('resposta não JSON produz erro alternativo seguro', async () => {
  const api = new ApiClient(undefined, async () => new Response('<html>erro</html>', { status: 502 }));
  await assert.rejects(api.login(credentials), error => error.status === 502 && !error.message.includes('<html>'));
});

test('catálogo de ativos normaliza parâmetros e não envia segredos no corpo', async () => {
  const calls = [];
  const api = new ApiClient(undefined, async (url, init) => { calls.push({url,init}); return calls.length === 1 ? loginResponse() : json({itens:[],pagina:1,tamanho:20,total:0,atualizadoEm:'2026-09-06T12:00:00Z',desatualizado:false}); });
  await api.login(credentials);await api.ativos(' petr ',1,20);
  assert.equal(calls[1].url,'http://localhost:8080/api/ativos?busca=PETR&pagina=1&tamanho=20');
  assert.equal(calls[1].init.body,undefined);assert.equal(calls[1].init.headers.Authorization,'Bearer token-falso-de-teste');
});
