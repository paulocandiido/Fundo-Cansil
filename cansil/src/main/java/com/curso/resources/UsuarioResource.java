package com.curso.resources;

import com.curso.domains.dtos.AuthDTOs.UsuarioResponse;
import com.curso.services.UsuarioAtualService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioResource {
    private final UsuarioAtualService usuarioAtual;

    public UsuarioResource(UsuarioAtualService usuarioAtual) { this.usuarioAtual = usuarioAtual; }

    @GetMapping("/me")
    public UsuarioResponse me() {
        var usuario = usuarioAtual.obter();
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail());
    }
}
