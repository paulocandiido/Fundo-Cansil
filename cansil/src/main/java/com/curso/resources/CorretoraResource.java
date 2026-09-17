package com.curso.resources;
import com.curso.domains.dtos.InvestimentoDTOs.*;import com.curso.services.CorretoraService;import java.util.*;import org.springframework.web.bind.annotation.*;import org.springframework.http.*;import jakarta.validation.Valid;
@RestController @RequestMapping("/api/corretoras") public class CorretoraResource {
 private final CorretoraService service;
 public CorretoraResource(CorretoraService s){service=s;}
 @GetMapping public List<CorretoraResponse> listar(){return service.listar();}
 @DeleteMapping("/{id}") public ResponseEntity<Void> remover(@PathVariable Long id){service.remover(id);return ResponseEntity.noContent().build();}
 @GetMapping("/cnpj/{cnpj}") public CnpjResponse consultar(@PathVariable String cnpj){return service.consultar(cnpj);}
 @PostMapping public ResponseEntity<CorretoraResponse> cadastrar(@Valid @RequestBody CorretoraRequest d){return ResponseEntity.status(HttpStatus.CREATED).body(service.cadastrar(d));}
}
