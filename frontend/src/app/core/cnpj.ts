export function normalizarCnpj(valor: string): string {
  return valor.trim().toUpperCase().replace(/[./-]/g, '');
}

export function cnpjValido(valor: string): boolean {
  const entrada = valor.trim().toUpperCase();
  if (!/^([A-Z0-9]{12}[0-9]{2}|[A-Z0-9]{2}\.[A-Z0-9]{3}\.[A-Z0-9]{3}\/[A-Z0-9]{4}-[0-9]{2})$/.test(entrada)) return false;
  const cnpj = normalizarCnpj(entrada);
  if (/^([0-9])\1{13}$/.test(cnpj)) return false;
  const digito = (base: string) => {
    let soma = 0, peso = 2;
    for (let i = base.length - 1; i >= 0; i--) {
      soma += (base.charCodeAt(i) - 48) * peso;
      peso = peso === 9 ? 2 : peso + 1;
    }
    const resto = soma % 11;
    return resto < 2 ? 0 : 11 - resto;
  };
  return digito(cnpj.slice(0, 12)) === Number(cnpj[12]) && digito(cnpj.slice(0, 13)) === Number(cnpj[13]);
}
