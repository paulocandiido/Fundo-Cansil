package com.curso.cotacao;
import com.curso.domains.enums.FonteCotacao;
public interface CotacaoProvider { FonteCotacao fonte(); CotacaoAtual buscar(String simbolo); }
