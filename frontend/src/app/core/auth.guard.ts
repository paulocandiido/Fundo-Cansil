import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Sessao } from './sessao';

// A autorização real continua no backend; o guard organiza apenas a navegação.
export const autenticado: CanActivateFn = () => inject(Sessao).usuario() ? true : inject(Router).createUrlTree(['/login']);
export const visitante: CanActivateFn = () => inject(Sessao).usuario() ? inject(Router).createUrlTree(['/carteira']) : true;
