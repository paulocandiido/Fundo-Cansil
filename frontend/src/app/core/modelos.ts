export interface Usuario { id: number; nome: string; email: string }
export interface Credenciais { email: string; senha: string }
export interface Cadastro extends Credenciais { nome: string; cpf: string }
export interface TokenResponse { token: string; tipo: string; expiraEmSegundos: number }
export type Mercado = 'BR' | 'USA';
export interface Corretora { id: number; codigo: string; nome: string; ativa: boolean; legada: boolean; cnpj: string | null; mercado?: Mercado; verificadoEm: string | null; removida?: boolean }
export interface ConsultaCnpj { cnpj: string; razaoSocial: string; situacaoCadastral: string; ativa: boolean; consultadoEm: string }
export interface NovaCorretora { cnpj: string; mercado?: Mercado; corretoraLegadaId?: number }
export interface AtivoCatalogo { simbolo: string }
export interface PaginaAtivos { itens: AtivoCatalogo[]; pagina: number; tamanho: number; total: number; atualizadoEm: string; desatualizado: boolean }
export interface Posicao { moeda?: string; simbolo: string; quantidade: number; precoMedio: number; custoTotal: number }
export interface PosicaoCorretora extends Posicao { corretora: Corretora }
export type TipoTransacao = 'COMPRA' | 'VENDA';
export interface NovaTransacao { mercado: Mercado; simbolo: string; corretoraId: number; tipo: TipoTransacao; quantidade: string; valorUnitario: string }
export interface Transacao { moeda?: string; id: number; simbolo: string; corretora: Corretora; tipo: TipoTransacao; quantidade: number; valorUnitario: number; dataOperacao: string }
export interface Cotacao { moeda?: string; simbolo: string; nome: string | null; bolsa: string | null; preco: number; fonte: string }
export interface Consulta { moeda?: string; id: number; simbolo: string; valor: number; fonte: string; consultadoEm: string }
export interface Avaliacao extends Posicao { cotacaoAtual: number; fonte: string; custoTotal: number; valorAtual: number; lucroPrejuizo: number; lucroPrejuizoPercentual: number }
