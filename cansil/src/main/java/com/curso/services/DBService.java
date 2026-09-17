package com.curso.services;

import com.curso.domains.GrupoProduto;
import com.curso.domains.Produto;
import com.curso.domains.enums.Status;
import com.curso.repositories.GrupoProdutoRepository;
import com.curso.repositories.ProdutoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
public class DBService {

    @Autowired
    private GrupoProdutoRepository grupoProdutoRepo;

    @Autowired
    private ProdutoRepository produtoRepo;

    public void initDB(){

        // Com o Liquibase o banco nao e mais apagado a cada inicializacao.
        // Se ja existem grupos cadastrados, evitamos inserir os exemplos novamente.
        if (grupoProdutoRepo.count() > 0) {
            return;
        }

        GrupoProduto grupo01 = new GrupoProduto(null,"Limpeza", Status.ATIVO);
        GrupoProduto grupo02 = new GrupoProduto(null,"Alimenticio",Status.ATIVO);

        Produto produto01 = new Produto(null,"1111","Coca Cola",new BigDecimal("100"),new BigDecimal("3.5"),
                LocalDate.now(),grupo02,Status.ATIVO);
        Produto produto02 = new Produto(null,"2222","Guarana Antartica",new BigDecimal("200"),new BigDecimal("3.0"),
                LocalDate.now(),grupo02,Status.ATIVO);
        Produto produto03 = new Produto(null,"3333","Detergente Limpol",new BigDecimal("300"),new BigDecimal("4.0"),
                LocalDate.now(),grupo01,Status.ATIVO);
        Produto produto04 = new Produto(null,"4444","Sabão em Pó OMO",new BigDecimal("400"),new BigDecimal("15.5"),
                LocalDate.now(),grupo02,Status.ATIVO);

        grupoProdutoRepo.save(grupo01);
        grupoProdutoRepo.save(grupo02);
        produtoRepo.save(produto01);
        produtoRepo.save(produto02);
        produtoRepo.save(produto03);
        produtoRepo.save(produto04);

    }

}
