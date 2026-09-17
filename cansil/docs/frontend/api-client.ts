/** Referência sem framework. JWT somente em memória; nenhuma chave de provider pertence ao front. */
export type FonteCotacao = 'BRAPI' | 'ALPHA_VANTAGE' | 'HG_FINANCE';
export type TipoTransacao = 'COMPRA' | 'VENDA';
export interface Usuario { id: number; nome: string; email: string }
export interface Cadastro { nome: string; email: string; cpf: string; senha: string }
export interface Login { email: string; senha: string }
export interface TokenResponse { token: string; tipo: string; expiraEmSegundos: number }
export interface Cotacao { moeda: string; simbolo: string; nome: string | null; bolsa: string | null; preco: number; fonte: FonteCotacao }
export interface Consulta { moeda: string; id: number; simbolo: string; valor: number; fonte: FonteCotacao; consultadoEm: string }
export type Mercado = 'BR' | 'USA';
export interface Corretora { id: number; codigo: string; nome: string; ativa: boolean; legada: boolean; cnpj: string | null; mercado?: Mercado; verificadoEm: string | null; removida?: boolean }
export interface ConsultaCnpj { cnpj: string; razaoSocial: string; situacaoCadastral: string; ativa: boolean; consultadoEm: string }
export interface NovaCorretora { cnpj: string; mercado?: Mercado; corretoraLegadaId?: number }
export interface PaginaAtivos { itens: { simbolo: string }[]; pagina: number; tamanho: number; total: number; atualizadoEm: string; desatualizado: boolean }
// Envie decimais como strings com ponto para evitar arredondamento ao montar o pedido.
export interface NovaTransacao { mercado: Mercado; simbolo: string; corretoraId: number; tipo: TipoTransacao; quantidade: string; valorUnitario: string }
export interface Transacao { moeda: string; id: number; simbolo: string; corretora: Corretora; tipo: TipoTransacao; quantidade: number; valorUnitario: number; dataOperacao: string }
export interface Posicao { moeda: string; simbolo: string; quantidade: number; precoMedio: number; custoTotal: number }
export interface PosicaoCorretora extends Posicao { corretora: Corretora }
export interface Avaliacao extends Posicao { cotacaoAtual: number; fonte: FonteCotacao; custoTotal: number; valorAtual: number; lucroPrejuizo: number; lucroPrejuizoPercentual: number }
export interface StandardError { timeStamp: number; status: number; error: string; message: string; path: string }

export class ApiError extends Error {
  readonly status: number;
  constructor(status: number, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

export class ApiClient {
  private token: string | null = null;
  private readonly baseUrl: string;
  private readonly transport: typeof fetch;

  constructor(baseUrl = 'http://localhost:8080', transport: typeof fetch = globalThis.fetch.bind(globalThis)) {
    this.baseUrl = baseUrl.replace(/\/+$/, '');
    this.transport = transport;
  }

  logout(): void { this.token = null; }
  cadastrar(dados: Cadastro): Promise<Usuario> { return this.request('/api/auth/cadastro', 'POST', dados, false); }
  async login(dados: Login): Promise<TokenResponse> {
    this.logout();
    const response = await this.request<TokenResponse>('/api/auth/login', 'POST', dados, false);
    this.token = response.token;
    return response;
  }
  me(): Promise<Usuario> { return this.request('/api/usuarios/me'); }
  cotacao(simbolo: string): Promise<Cotacao> { return this.request(`/api/cotacoes/${encodeURIComponent(simbolo.trim().toUpperCase())}`); }
  historicoConsultas(): Promise<Consulta[]> { return this.request('/api/cotacoes/historico'); }
  transacoes(): Promise<Transacao[]> { return this.request('/api/transacoes'); }
  registrarTransacao(dados: NovaTransacao): Promise<Transacao> { return this.request('/api/transacoes', 'POST', dados); }
  corretoras(): Promise<Corretora[]> { return this.request('/api/corretoras'); }
  consultarCnpj(cnpj: string): Promise<ConsultaCnpj> { return this.request('/api/corretoras/cnpj/' + encodeURIComponent(cnpj.trim().toUpperCase().replace(/[./-]/g, ''))); }
  cadastrarCorretora(dados: NovaCorretora): Promise<Corretora> { return this.request('/api/corretoras', 'POST', dados); }
  removerCorretora(id: number): Promise<void> { return this.request('/api/corretoras/' + id, 'DELETE'); }
  ativos(busca: string, pagina = 0, tamanho = 20): Promise<PaginaAtivos> {
    const query = new URLSearchParams({ busca: busca.trim().toUpperCase(), pagina: String(pagina), tamanho: String(tamanho) });
    return this.request(`/api/ativos?${query.toString()}`);
  }
  carteira(): Promise<Posicao[]> { return this.request('/api/carteira'); }
  carteiraPorCorretora(): Promise<PosicaoCorretora[]> { return this.request('/api/carteira/corretoras'); }
  avaliacao(): Promise<Avaliacao[]> { return this.request('/api/carteira/avaliacao'); }

  private async request<T>(path: string, method = 'GET', body?: unknown, authenticated = true): Promise<T> {
    const sentToken = authenticated ? this.token : null;
    if (authenticated && !sentToken) throw new ApiError(401, 'Faça login para continuar.');
    const headers: Record<string, string> = { Accept: 'application/json' };
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    if (sentToken) headers.Authorization = `Bearer ${sentToken}`;
    let response: Response;
    try {
      response = await this.transport(`${this.baseUrl}${path}`, {
        method, headers, credentials: 'omit', cache: 'no-store',
        body: body === undefined ? undefined : JSON.stringify(body),
      });
    } catch {
      // Não repete POST automaticamente: uma falha de rede pode ocorrer depois da gravação.
      throw new ApiError(0, 'Não foi possível acessar a API. Verifique conexão, Docker e configuração CORS.');
    }
    if (response.status === 401 && authenticated && this.token === sentToken) this.logout();
    if (response.status === 204 && method === 'DELETE') return undefined as T;
    const payload: unknown = await response.json().catch(() => null);
    if (!response.ok) {
      const message = payload && typeof payload === 'object' && 'message' in payload && typeof payload.message === 'string'
        ? payload.message : `Falha na API (HTTP ${response.status}).`;
      throw new ApiError(response.status, message);
    }
    if (payload === null) throw new ApiError(response.status, 'Resposta JSON ausente ou inválida.');
    return payload as T;
  }
}
