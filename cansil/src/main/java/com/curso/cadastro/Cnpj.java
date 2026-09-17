package com.curso.cadastro;

import java.util.Locale;

public final class Cnpj {
    private Cnpj() {}
    public static String normalizar(String valor) {
        String cnpj = valor == null ? "" : valor.trim().toUpperCase(Locale.ROOT);
        if (!cnpj.matches("[A-Z0-9]{12}[0-9]{2}|[A-Z0-9]{2}\\.[A-Z0-9]{3}\\.[A-Z0-9]{3}/[A-Z0-9]{4}-[0-9]{2}"))
            throw new IllegalArgumentException("Informe um CNPJ válido, com ou sem formatação");
        cnpj = cnpj.replace(".", "").replace("/", "").replace("-", "");
        if (cnpj.matches("([0-9])\\1{13}") || digito(cnpj.substring(0, 12)) != cnpj.charAt(12) - '0'
                || digito(cnpj.substring(0, 13)) != cnpj.charAt(13) - '0')
            throw new IllegalArgumentException("Os dígitos verificadores do CNPJ são inválidos");
        return cnpj;
    }
    private static int digito(String base) {
        int soma = 0, peso = 2;
        for (int i = base.length() - 1; i >= 0; i--) {
            soma += (base.charAt(i) - 48) * peso;
            peso = peso == 9 ? 2 : peso + 1;
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
