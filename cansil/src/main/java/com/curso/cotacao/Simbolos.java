package com.curso.cotacao;
import java.util.Locale; import java.util.regex.Pattern;
public final class Simbolos { private static final Pattern P=Pattern.compile("[A-Z0-9][A-Z0-9.-]{0,19}"); private Simbolos(){} public static String normalizar(String valor){String s=valor==null?"":valor.trim().toUpperCase(Locale.ROOT);if(!P.matcher(s).matches())throw new IllegalArgumentException("Símbolo inválido");return s;} }
