package com.curso.services.exceptions;

public class CadastroIndisponivelException extends RuntimeException {
    public CadastroIndisponivelException() {
        super("A consulta cadastral está indisponível ou não retornou dados válidos. Tente novamente mais tarde.");
    }
}
