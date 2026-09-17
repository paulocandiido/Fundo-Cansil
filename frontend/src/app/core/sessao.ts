import { Injectable, signal } from '@angular/core';
import { Usuario } from './modelos';

@Injectable({ providedIn: 'root' })
export class Sessao {
  private token: string | null = null;
  private readonly identidade = signal<Usuario | null>(null);
  readonly usuario = this.identidade.asReadonly();
  obterToken() { return this.token; }
  definirToken(token: string) { this.token = token; }
  identificar(usuario: Usuario) { this.identidade.set(usuario); }
  limpar() { this.token = null; this.identidade.set(null); }
}
