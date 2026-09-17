package com.curso.resources;
import com.curso.domains.dtos.InvestimentoDTOs.*;import com.curso.services.CotacaoService;import org.springframework.web.bind.annotation.*;import java.util.List;
@RestController @RequestMapping("/api/cotacoes") public class CotacaoResource{private final CotacaoService s;public CotacaoResource(CotacaoService s){this.s=s;}@GetMapping("/{simbolo}")public CotacaoResponse buscar(@PathVariable String simbolo){return s.buscar(simbolo);}@GetMapping("/historico")public List<ConsultaResponse> historico(){return s.historico();}}
