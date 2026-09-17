import { Component, DestroyRef, inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { Sessao } from './core/sessao';
import { registrarNavegacao } from './core/navegacao-assistida';
import { OperacaoEmEnvio } from './core/operacao-em-envio';

@Component({
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  selector: 'app-root',
  template: `
    <a class="skip-link" href="#conteudo">Pular para o conteúdo</a>
    <header class="topbar">
      <a class="brand" routerLink="/carteira" aria-label="Fundo Cansil, início"><span class="brand-mark" aria-hidden="true">FC</span><span>Fundo <span class="brand-light">Cansil</span></span></a>
      @if (sessao.usuario(); as usuario) {
        <div class="user-menu"><span class="user-name">{{ usuario.nome }}</span><button class="button button-quiet" type="button" [disabled]="operacaoEmEnvio.ativa()" (click)="sair()">Sair</button></div>
      } @else { <span class="environment">Gestão de investimentos</span> }
    </header>
    @if (sessao.usuario()) { <nav class="main-nav" aria-label="Navegação principal"><a routerLink="/carteira" routerLinkActive="active" ariaCurrentWhenActive="page">Minha carteira</a><a routerLink="/operacoes" routerLinkActive="active" ariaCurrentWhenActive="page">Compras e vendas</a><a routerLink="/cotacoes" routerLinkActive="active" ariaCurrentWhenActive="page">Cotações</a><a routerLink="/corretoras" routerLinkActive="active" ariaCurrentWhenActive="page">Corretoras</a></nav> }
    <main id="conteudo" tabindex="-1"><router-outlet /></main>
    <footer class="footer">Fundo Cansil <span>Gestão de investimentos</span></footer>
  `,
})
export class App {
  readonly sessao = inject(Sessao);
  readonly operacaoEmEnvio = inject(OperacaoEmEnvio);
  private readonly router = inject(Router);
  constructor() {
    const cleanup = registrarNavegacao(inject(DOCUMENT) as Parameters<typeof registrarNavegacao>[0],
      () => !!this.sessao.usuario(), () => this.router.navigateByUrl('/carteira'));
    inject(DestroyRef).onDestroy(cleanup);
  }
  sair() { if (this.operacaoEmEnvio.ativa()) return; this.sessao.limpar(); void this.router.navigateByUrl('/login'); }
}
