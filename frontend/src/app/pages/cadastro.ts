import { Component, DestroyRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { finalize } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { Api } from '../core/api';
import { limiteSenhaUtf8, senhasIguais } from '../core/validadores';

@Component({
  selector: 'app-cadastro', imports: [ReactiveFormsModule, RouterLink],
  template: `
    <section class="auth-layout registration">
      <div class="auth-intro"><span class="eyebrow">COMECE PELA SUA CONTA</span><h1>Um lugar para<br>acompanhar sua carteira.</h1><p>Cadastre-se para consultar seus ativos e manter seus registros separados.</p><div class="intro-note"><span class="note-line"></span><p>Já possui uma conta?<br><a routerLink="/login">Voltar para o login</a></p></div></div>
      <div class="auth-card"><div class="section-number" aria-hidden="true">02 / CADASTRO</div><h2>Criar sua conta</h2><p class="muted">Todos os campos são obrigatórios.</p>
        @if (erro()) { <p class="notice error" role="alert">{{ erro() }}</p> }
        <form [formGroup]="form" (ngSubmit)="enviar()" novalidate [attr.aria-busy]="enviando()">
          <div class="field"><label for="nome">Nome</label><input id="nome" formControlName="nome" autocomplete="name" maxlength="120" [attr.aria-invalid]="invalido('nome')" aria-describedby="nome-erro"><small id="nome-erro" class="field-error">@if (invalido('nome')) { Informe seu nome, com até 120 caracteres. }</small></div>
          <div class="field"><label for="cadastro-email">E-mail</label><input id="cadastro-email" type="email" formControlName="email" autocomplete="email" maxlength="160" placeholder="voce@exemplo.com" [attr.aria-invalid]="invalido('email')" aria-describedby="cadastro-email-erro"><small id="cadastro-email-erro" class="field-error">@if (invalido('email')) { Informe um e-mail válido. }</small></div>
          <div class="field"><label for="cpf">CPF</label><input id="cpf" formControlName="cpf" inputmode="numeric" maxlength="14" placeholder="000.000.000-00" [attr.aria-invalid]="invalido('cpf')" aria-describedby="cpf-erro"><small id="cpf-erro" class="field-error">@if (invalido('cpf')) { Use 11 dígitos ou o formato 000.000.000-00. }</small></div>
          <div class="field"><label for="nova-senha">Senha</label><input id="nova-senha" type="password" formControlName="senha" autocomplete="new-password" [attr.aria-invalid]="invalido('senha')" aria-describedby="senha-ajuda nova-senha-erro"><small id="senha-ajuda" class="field-hint">Use pelo menos 8 caracteres.</small><small id="nova-senha-erro" class="field-error">@if (invalido('senha')) { Informe uma senha válida com pelo menos 8 caracteres. }</small></div>
          <div class="field"><label for="confirmacao">Confirmar senha</label><input id="confirmacao" type="password" formControlName="confirmacao" autocomplete="new-password" [attr.aria-invalid]="confirmacaoInvalida()" aria-describedby="confirmacao-erro"><small id="confirmacao-erro" class="field-error">@if (confirmacaoInvalida()) { As senhas precisam ser iguais. }</small></div>
          <button type="submit" class="button button-primary full" [disabled]="enviando()">{{ enviando() ? 'Criando conta…' : 'Criar conta' }}<span aria-hidden="true">↗</span></button>
        </form>
        <p class="auth-switch">Já tem uma conta? <a routerLink="/login">Entrar</a></p>
      </div>
    </section>
  `,
})
export class CadastroPage {
  private readonly api = inject(Api);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  readonly enviando = signal(false);
  readonly erro = signal('');
  readonly form = inject(FormBuilder).nonNullable.group({
    nome: ['', [Validators.required, Validators.maxLength(120)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(160)]],
    cpf: ['', [Validators.required, Validators.pattern(/^(?:\d{11}|\d{3}\.\d{3}\.\d{3}-\d{2})$/)]],
    senha: ['', [Validators.required, Validators.minLength(8), limiteSenhaUtf8]],
    confirmacao: ['', Validators.required],
  }, { validators: senhasIguais });
  invalido(campo: keyof typeof this.form.controls) { const control = this.form.controls[campo]; return control.touched && control.invalid; }
  confirmacaoInvalida() { return this.form.controls.confirmacao.touched && (this.form.controls.confirmacao.invalid || this.form.hasError('senhasDiferentes')); }
  enviar() {
    if (this.enviando()) return;
    for (const campo of ['nome', 'email', 'cpf'] as const) this.form.controls[campo].setValue(this.form.controls[campo].value.trim());
    this.form.markAllAsTouched();
    if (this.form.invalid) return;
    const { confirmacao, ...dados } = this.form.getRawValue();
    this.erro.set(''); this.enviando.set(true);
    this.api.cadastrar(dados).pipe(takeUntilDestroyed(this.destroyRef), finalize(() => this.enviando.set(false)))
      .subscribe({ next: () => { this.form.reset(); void this.router.navigate(['/login'], { queryParams: { cadastro: 'sucesso' } }); }, error: (erro: Error) => this.erro.set(erro.message) });
  }
}
