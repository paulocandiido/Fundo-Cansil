import { Routes } from '@angular/router';
import { LoginPage } from './pages/login';
import { CadastroPage } from './pages/cadastro';
import { CarteiraPage } from './pages/carteira';
import { autenticado, visitante } from './core/auth.guard';
import { OperacoesPage } from './pages/operacoes';
import { CotacoesPage } from './pages/cotacoes';
import { CorretorasPage } from './pages/corretoras';

export const routes: Routes = [
  { path: 'corretoras', component: CorretorasPage, canActivate: [autenticado], canDeactivate: [(page: CorretorasPage) => !page.enviando()], title: 'Minhas corretoras | Fundo Cansil' },
  { path: 'login', component: LoginPage, canActivate: [visitante], title: 'Entrar | Fundo Cansil' },
  { path: 'cadastro', component: CadastroPage, canActivate: [visitante], title: 'Criar conta | Fundo Cansil' },
  { path: 'carteira', component: CarteiraPage, canActivate: [autenticado], title: 'Minha carteira | Fundo Cansil' },
  { path: 'operacoes', component: OperacoesPage, canActivate: [autenticado], canDeactivate: [(page: OperacoesPage) => !page.enviando()], title: 'Compras e vendas | Fundo Cansil' },
  { path: 'cotacoes', component: CotacoesPage, canActivate: [autenticado], title: 'Cotações | Fundo Cansil' },
  { path: '', pathMatch: 'full', redirectTo: 'carteira' },
  { path: '**', redirectTo: 'login' },
];
