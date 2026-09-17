import { ValidatorFn } from '@angular/forms';
import { Pipe, PipeTransform } from '@angular/core';

export const simboloValido: ValidatorFn = control => /^[A-Za-z0-9][A-Za-z0-9.-]{0,19}$/.test(String(control.value).trim()) ? null : { simbolo: true };
export const decimalPositivo: ValidatorFn = control => {
  const valor = String(control.value).trim();
  return /^\d{1,13}(?:[.,]\d{1,6})?$/.test(valor) && /[1-9]/.test(valor) ? null : { decimal: true };
};
export function decimalParaApi(valor: string): string { return valor.trim().replace(',', '.'); }
export const dataLocalValida: ValidatorFn = control => {
  const partes = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2}))?$/.exec(String(control.value));
  if (!partes) return { dataLocal: true };
  const [ano, mes, dia, hora, minuto, segundo] = partes.slice(1).map(p => Number(p ?? 0));
  const bissexto = ano % 4 === 0 && (ano % 100 !== 0 || ano % 400 === 0);
  const dias = [31, bissexto ? 29 : 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31];
  return ano >= 1 && mes >= 1 && mes <= 12 && dia >= 1 && dia <= dias[mes - 1] && hora < 24 && minuto < 60 && segundo < 60 ? null : { dataLocal: true };
};
export function dataParaApi(valor: string): string { return valor.length === 16 ? valor + ':00' : valor; }

@Pipe({ name: 'dataLocal' })
export class DataLocalPipe implements PipeTransform {
  transform(valor: string): string {
    const partes = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2}))?/.exec(valor);
    return partes ? `${partes[3]}/${partes[2]}/${partes[1]} ${partes[4]}:${partes[5]}:${partes[6] ?? '00'}` : valor;
  }
}
