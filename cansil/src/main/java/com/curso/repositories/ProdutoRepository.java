package com.curso.repositories;

import com.curso.domains.Produto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProdutoRepository extends JpaRepository<Produto, Long> {

    Page<Produto> findByGrupoProduto_Id(Integer grupoId, Pageable pageable);

    Optional<Produto> findByCodigoBarra(String codigoBarra);

    boolean existsByGrupoProduto_Id(Integer grupoId);
}

