package com.curso.resources;

import com.curso.domains.GrupoProduto;
import com.curso.domains.dtos.GrupoProdutoDTO;
import com.curso.mappers.GrupoProdutoMapper;
import com.curso.services.GrupoProdutoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
@RequestMapping("/api/grupoproduto")
public class GrupoProdutoResource {

    private final GrupoProdutoService service;

    public GrupoProdutoResource(GrupoProdutoService service) {
        this.service = service;
    }

    // GET não paginado (simples e direto)
    @GetMapping("/all")
    public ResponseEntity<List<GrupoProdutoDTO>> listAll() {
        return ResponseEntity.ok(service.findAll());
    }

    // GET "paginado" embrulhado (usa findAll e monta PageImpl)
    @GetMapping
    public ResponseEntity<Page<GrupoProdutoDTO>> list(
            @PageableDefault(size = 20, sort = "descricao") Pageable pageable) {
        List<GrupoProdutoDTO> all = service.findAll();
        Page<GrupoProdutoDTO> page = new PageImpl<>(all, pageable, all.size());
        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    public ResponseEntity<GrupoProdutoDTO> findById(@PathVariable Integer id) {
        GrupoProdutoDTO dto = service.findById(id);
        return ResponseEntity.ok(dto);
    }

    @PostMapping
    public ResponseEntity<GrupoProdutoDTO> create(@RequestBody @Validated(GrupoProdutoDTO.Create.class) GrupoProdutoDTO dto) {
        GrupoProdutoDTO created = service.create(dto);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GrupoProdutoDTO> update(
            @PathVariable Integer id,
            @RequestBody @Validated(GrupoProdutoDTO.Update.class) GrupoProdutoDTO dto) {
        dto.setId(id);
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
