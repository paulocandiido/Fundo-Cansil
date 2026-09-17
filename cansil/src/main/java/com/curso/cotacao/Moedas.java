package com.curso.cotacao;

import java.util.Currency;
import java.util.Locale;
import com.curso.services.exceptions.RegraNegocioException;

public final class Moedas {
    private Moedas() {}
    public static String normalizar(String valor) {
        if (valor == null) throw new IllegalArgumentException("Moeda não informada");
        return Currency.getInstance(valor.trim().toUpperCase(Locale.ROOT)).getCurrencyCode();
    }
    public static void conferir(String esperada, String recebida) {
        if (!normalizar(esperada).equals(normalizar(recebida)))
            throw new RegraNegocioException("O ativo não pertence ao mercado selecionado. BR usa BRL e USA usa USD.");
    }
}
