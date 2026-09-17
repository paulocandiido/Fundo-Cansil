import { inject, Injectable, InjectionToken } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { catchError, Observable, switchMap, tap, throwError, timeout } from 'rxjs';
import { environment } from '../../environments/environment';
import { Avaliacao, Cadastro, Consulta, ConsultaCnpj, Corretora, Cotacao, Credenciais, NovaCorretora, NovaTransacao, PaginaAtivos, Posicao, PosicaoCorretora, TokenResponse, Transacao, Usuario } from './modelos';
import { Sessao } from './sessao';

export const API_URL = new InjectionToken<string>('API_URL', { providedIn: 'root', factory: () => environment.apiUrl });
export class ErroApi extends Error {
  constructor(readonly status: number, message: string, readonly resultadoIncerto = false) { super(message); }
}

@Injectable({ providedIn: 'root' })
export class Api {
  private readonly http = inject(HttpClient);
  private readonly sessao = inject(Sessao);
  private readonly base = inject(API_URL).replace(/\/+$/, '');

  cadastrar(dados: Cadastro) { return this.pedido<Usuario>('POST', '/api/auth/cadastro', dados, false); }
  entrar(dados: Credenciais): Observable<Usuario> {
    this.sessao.limpar();
    return this.pedido<TokenResponse>('POST', '/api/auth/login', dados, false).pipe(
      tap(resposta => this.sessao.definirToken(resposta.token)),
      switchMap(() => this.me()),
      tap(usuario => this.sessao.identificar(usuario)),
      catchError(erro => { this.sessao.limpar(); return throwError(() => erro); }),
    );
  }
  me() { return this.pedido<Usuario>('GET', '/api/usuarios/me'); }
  carteira() { return this.pedido<Posicao[]>('GET', '/api/carteira'); }
  carteiraPorCorretora() { return this.pedido<PosicaoCorretora[]>('GET', '/api/carteira/corretoras'); }
  corretoras() { return this.pedido<Corretora[]>('GET', '/api/corretoras'); }
  consultarCnpj(cnpj: string) { return this.pedido<ConsultaCnpj>('GET', '/api/corretoras/cnpj/' + encodeURIComponent(cnpj)); }
  cadastrarCorretora(dados: NovaCorretora) { return this.pedido<Corretora>('POST', '/api/corretoras', dados); }
  removerCorretora(id: number) { return this.pedido<void>('DELETE', '/api/corretoras/' + id); }
  ativos(busca: string, pagina = 0, tamanho = 20) {
    const params = new URLSearchParams({ busca: busca.trim().toUpperCase(), pagina: String(pagina), tamanho: String(tamanho) });
    return this.pedido<PaginaAtivos>('GET', '/api/ativos?' + params.toString());
  }
  transacoes() { return this.pedido<Transacao[]>('GET', '/api/transacoes'); }
  registrarTransacao(dados: NovaTransacao) { return this.pedido<Transacao>('POST', '/api/transacoes', dados); }
  cotacao(simbolo: string) { return this.pedido<Cotacao>('GET', '/api/cotacoes/' + encodeURIComponent(simbolo.trim().toUpperCase())); }
  historicoConsultas() { return this.pedido<Consulta[]>('GET', '/api/cotacoes/historico'); }
  avaliar() { return this.pedido<Avaliacao[]>('GET', '/api/carteira/avaliacao'); }

  private pedido<T>(method: string, path: string, body?: unknown, protegido = true): Observable<T> {
    const token = protegido ? this.sessao.obterToken() : null;
    if (protegido && !token) return throwError(() => new ErroApi(401, 'Entre na sua conta para continuar.'));
    const headers: Record<string, string> = { Accept: 'application/json' };
    if (token) headers['Authorization'] = `Bearer ${token}`;
    return this.http.request<T>(method, this.base + path, { body, headers, withCredentials: false }).pipe(
      timeout(path === '/api/carteira/avaliacao' ? 60000 : 15000),
      catchError((erro: unknown) => {
        const status = erro instanceof HttpErrorResponse ? erro.status : 0;
        const login = path === '/api/auth/login';
        if (status === 401 && protegido && token === this.sessao.obterToken()) this.sessao.limpar();
        const mensagens: Record<number, string> = {
          0: 'Não foi possível acessar o servidor. Verifique sua conexão e se o backend está iniciado.',
          400: 'Confira os campos informados e tente novamente.',
          401: protegido ? 'Sua sessão expirou. Entre novamente.' : 'E-mail ou senha incorretos.',
          403: 'Acesso não permitido. Verifique a configuração da aplicação.',
          409: 'Já existe uma conta com esse e-mail ou CPF.',
          422: path === '/api/transacoes' ? 'Confira se o ativo pertence ao mercado selecionado e, nas vendas, o saldo disponível na corretora e a quantidade informada.' : 'A operação não pôde ser concluída. Confira os dados.',
          503: 'Cotações indisponíveis no momento. Tente novamente mais tarde.',
        };
        if (login) {
          const indisponivel = 'O servidor está iniciando ou temporariamente indisponível. Aguarde alguns segundos e tente novamente.';
          mensagens[0] = indisponivel;
          mensagens[500] = 'Não foi possível concluir o login agora. Aguarde alguns segundos e tente novamente.';
          mensagens[502] = indisponivel;
          mensagens[503] = indisponivel;
          mensagens[504] = indisponivel;
        }
        if (path.startsWith('/api/corretoras')) {
          mensagens[400] = 'Confira o CNPJ e se a corretora pertence à sua conta.';
          mensagens[404] = 'CNPJ não encontrado na fonte cadastral.';
          mensagens[409] = 'Este CNPJ já está cadastrado na sua conta. Utilize a corretora existente.';
          mensagens[503] = 'A consulta cadastral está indisponível. Tente novamente mais tarde.';
        }
        const incerto = ((method === 'POST' && (path === '/api/transacoes' || path === '/api/corretoras')) || (method === 'DELETE' && path.startsWith('/api/corretoras/'))) && (status === 0 || status >= 500 || (status >= 200 && status < 300));
        const mensagem = incerto
          ? (path.startsWith('/api/corretoras') ? 'Não foi possível confirmar a alteração. Atualize e confira a lista de corretoras antes de tentar novamente.' : 'Não foi possível confirmar o registro. A operação pode ter sido gravada. Atualize e confira o histórico antes de reenviar.')
          : mensagens[status] ?? 'O servidor não conseguiu concluir a solicitação. Tente novamente.';
        return throwError(() => new ErroApi(status, mensagem, incerto));
      }),
    );
  }
}
