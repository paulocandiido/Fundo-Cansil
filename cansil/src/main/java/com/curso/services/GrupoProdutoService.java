package com.curso.services;

import com.curso.domains.GrupoProduto;
import com.curso.domains.dtos.GrupoProdutoDTO;
import com.curso.mappers.GrupoProdutoMapper;
import com.curso.repositories.GrupoProdutoRepository;
import com.curso.repositories.ProdutoRepository;
import com.curso.services.exceptions.ObjectNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class GrupoProdutoService {

    private final GrupoProdutoRepository grupoProdutoRepo;
    private final ProdutoRepository produtoRepo;

    // Injeção por construtor (Spring injeta automaticamente se houver só um construtor público)
    public GrupoProdutoService(GrupoProdutoRepository grupoProdutoRepo, ProdutoRepository produtoRepo) {
        this.grupoProdutoRepo = grupoProdutoRepo;
        this.produtoRepo = produtoRepo;
    }

    @Transactional(readOnly = true)
    public List<GrupoProdutoDTO> findAll(){
        //retorna uma lista de ProdutoDTO
        return GrupoProdutoMapper.toDtoList(grupoProdutoRepo.findAll());
    }

    @Transactional(readOnly = true)
    public GrupoProdutoDTO findById(Integer id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "id é obrigatório");
        }

        return grupoProdutoRepo.findById(id)
                .map(GrupoProdutoMapper::toDto)
                .orElseThrow(() ->
                        new ObjectNotFoundException("Grupo de produto não encontrado: id=" + id));
    }

    @Transactional
    public GrupoProdutoDTO create(GrupoProdutoDTO grupoProdutoDTO) {

        if (grupoProdutoDTO == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados do grupo são obrigatórios");
        }

        grupoProdutoDTO.setId(null);
        GrupoProduto grupoProduto;
        try{
            grupoProduto = GrupoProdutoMapper.toEntity(grupoProdutoDTO);
        } catch (IllegalArgumentException ex){
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage());
        }

        return GrupoProdutoMapper.toDto(grupoProdutoRepo.save(grupoProduto));
    }

    @Transactional
    public GrupoProdutoDTO update(Integer id, GrupoProdutoDTO grupoProdutoDTO) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id é obrigatório");
        }

        if (grupoProdutoDTO == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Dados do grupo produto são obrigatórios");
        }

        GrupoProduto grupoProduto = grupoProdutoRepo.findById(id)
                .orElseThrow(() ->
                        new ObjectNotFoundException("Grupo de Produto não encontrado: id=" + id));

        GrupoProdutoMapper.copyToEntity(grupoProdutoDTO, grupoProduto);

        return GrupoProdutoMapper.toDto(grupoProdutoRepo.save(grupoProduto));
    }

    @Transactional
    public void delete(Integer id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Id é obrigatório");
        }

        GrupoProduto grupoProduto = grupoProdutoRepo.findById(id)
                .orElseThrow(() ->
                        new ObjectNotFoundException("Grupo de Produto não encontrado: id=" + id));

        if (produtoRepo.existsByGrupoProduto_Id(id)) {
            throw new com.curso.services.exceptions.GrupoComProdutosException(id);
        }

        grupoProdutoRepo.delete(grupoProduto);
    }

}
