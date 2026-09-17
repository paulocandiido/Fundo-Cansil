import { ValidatorFn } from '@angular/forms';

export const limiteSenhaUtf8: ValidatorFn = control =>
  typeof control.value === 'string' && new TextEncoder().encode(control.value).length > 72 ? { bytesSenha: true } : null;

export const senhasIguais: ValidatorFn = group =>
  group.get('senha')?.value !== group.get('confirmacao')?.value ? { senhasDiferentes: true } : null;
