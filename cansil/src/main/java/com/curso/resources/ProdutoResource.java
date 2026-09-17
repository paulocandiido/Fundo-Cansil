package com.curso.resources;

import com.curso.domains.dtos.ProdutoDTO;
import com.curso.services.ProdutoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/produto")
public class ProdutoResource {

    private final ProdutoService service;

    public ProdutoResource(ProdutoService service) {
        this.service = service;
    }

    // GET paginado; filtro por grupo opcional (?grupoId=)
    @GetMapping
    public ResponseEntity<Page<ProdutoDTO>> list(
            @RequestParam(required = false) Integer grupoId,
            @PageableDefault(size = 20, sort = "descricao") Pageable pageable) {

        Page<ProdutoDTO> page = (grupoId != null)
                ? service.findAllByGrupo(grupoId, pageable) // paginado + filtro
                : service.findAll(pageable);                // paginado sem filtro (real no DB)

        return ResponseEntity.ok(page);
    }

    // GET não paginado; filtro por grupo opcional (?grupoId=)
    @GetMapping("/all")
    public ResponseEntity<List<ProdutoDTO>> listAll(
            @RequestParam(required = false) Integer grupoId) {

        List<ProdutoDTO> body = (grupoId != null)
                ? service.findAllByGrupo(grupoId) // não paginado + filtro
                : service.findAll();              // não paginado sem filtro

        return ResponseEntity.ok(body);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProdutoDTO> findById(@PathVariable Long id) {
        ProdutoDTO dto = service.findById(id);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/codigobarra/{codigobarra}")
    public ResponseEntity<ProdutoDTO> findByCodigoBarra(@PathVariable String codigobarra) {
        ProdutoDTO dto = service.findByCodigoBarra(codigobarra);
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    public ResponseEntity<ProdutoDTO> create(
            @RequestBody @Validated(ProdutoDTO.Create.class) ProdutoDTO dto) {

        ProdutoDTO created = service.create(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getIdProduto())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ProdutoDTO> update(@PathVariable Long id,
            @RequestBody @Validated(ProdutoDTO.Update.class) ProdutoDTO dto) {
        dto.setIdProduto(id);
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

}
