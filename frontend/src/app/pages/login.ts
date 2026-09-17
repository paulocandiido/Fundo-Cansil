import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Api } from '../core/api';

@Component({
  selector: 'app-login', imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section class="auth-layout">
      <div class="auth-intro"><span class="eyebrow">SUA CARTEIRA, ORGANIZADA</span><h1>Seus investimentos.<br>Uma visão clara.</h1><p>Acesse suas posições e acompanhe o preço médio dos seus ativos.</p><div class="intro-note"><span class="note-line"></span><p>Seus registros ficam associados à sua conta, com acesso individual.</p></div></div>
      <div class="auth-card">
        <div class="section-number" aria-hidden="true">01 / ACESSO</div><h2>Entre na sua conta</h2><p class="muted">Bem-vindo de volta ao Fundo Cansil.</p>
        @if (cadastrado) { <p class="notice success" role="status">Conta criada. Entre com seu e-mail e senha.</p> }
        @if (expirada) { <p class="notice" role="status">Sua sessão terminou. Entre novamente para continuar.</p> }
        @if (erro()) { <p class="notice error" role="alert">{{ erro() }}</p> }
        <form [formGroup]="form" (ngSubmit)="enviar()" novalidate [attr.aria-busy]="enviando()">
          <div class="field"><label for="email">E-mail</label><input id="email" type="email" formControlName="email" autocomplete="username" placeholder="voce@exemplo.com" [attr.aria-invalid]="invalido('email')" aria-describedby="email-erro">
          <small id="email-erro" class="field-error">@if (invalido('email')) { Informe um e-mail válido. }</small></div>
          <div class="field"><label for="senha">Senha</label><input id="senha" type="password" formControlName="senha" autocomplete="current-password" [attr.aria-invalid]="invalido('senha')" aria-describedby="senha-erro">
          <small id="senha-erro" class="field-error">@if (invalido('senha')) { Informe sua senha. }</small></div>
          <button class="button button-primary full" type="submit" [disabled]="enviando()">{{ enviando() ? 'Entrando…' : 'Entrar' }}<span aria-hidden="true">↗</span></button>
        </form>
        <p class="auth-switch">Ainda não tem uma conta? <a routerLink="/cadastro">Criar conta</a></p>
      </div>
    </section>
  `,
})
export class LoginPage {
  private readonly api = inject(Api);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly query = inject(ActivatedRoute).snapshot.queryParamMap;
  readonly cadastrado = this.query.get('cadastro') === 'sucesso';
  readonly expirada = this.query.get('sessao') === 'expirada';
  readonly enviando = signal(false);
  readonly erro = signal('');
  readonly form = inject(FormBuilder).nonNullable.group({ email: ['', [Validators.required, Validators.email]], senha: ['', Validators.required] });
  invalido(campo: 'email' | 'senha') { const control = this.form.controls[campo]; return control.touched && control.invalid; }
  enviar() {
    if (this.enviando()) return;
    this.form.controls.email.setValue(this.form.controls.email.value.trim());
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    this.erro.set(''); this.enviando.set(true);
    this.api.entrar(this.form.getRawValue()).pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.enviando.set(false)))
      .subscribe({ next: () => { this.form.controls.senha.reset(); void this.router.navigateByUrl('/carteira'); }, error: (erro: Error) => { this.erro.set(erro.message); this.form.controls.senha.reset(); } });
  }
}
