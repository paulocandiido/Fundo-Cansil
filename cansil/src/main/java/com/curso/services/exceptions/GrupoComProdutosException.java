package com.curso.services.exceptions;

import org.springframework.dao.DataIntegrityViolationException;

/** Mensagem de negócio segura, distinta dos detalhes técnicos de SQL. */
public class GrupoComProdutosException extends DataIntegrityViolationException {
    public GrupoComProdutosException(Integer id) {
        super("Grupo de produto possui produtos associados e não pode ser removido: id=" + id);
    }
}
