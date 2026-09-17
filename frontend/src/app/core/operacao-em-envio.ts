import { Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class OperacaoEmEnvio {
  readonly ativa = signal(false);
}
