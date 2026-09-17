package com.curso.services;

import com.curso.cadastro.*;
import com.curso.domains.*;
import com.curso.domains.dtos.InvestimentoDTOs.*;
import com.curso.domains.enums.*;
import com.curso.repositories.CorretoraRepository;
import com.curso.repositories.TransacaoRepository;
import com.curso.services.exceptions.ConflitoException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CorretoraService {
    private final CorretoraRepository repo;
    private final UsuarioAtualService atual;
    private final BrasilApiCnpjClient cadastro;
    private final TransacaoRepository transacoes;
    public CorretoraService(CorretoraRepository repo, UsuarioAtualService atual, BrasilApiCnpjClient cadastro, TransacaoRepository transacoes) {
        this.repo=repo; this.atual=atual; this.cadastro=cadastro; this.transacoes=transacoes;
    }
    @Transactional(readOnly=true)
    public List<CorretoraResponse> listar() {
        return repo.findByUsuarioIdOrderByNomeAscIdAsc(atual.obter().getId()).stream().filter(c->!c.isRemovida()).map(CorretoraService::dto).toList();
    }
    public CnpjResponse consultar(String cnpj) { return cadastro.consultar(cnpj); }
    @Transactional
    public CorretoraResponse cadastrar(CorretoraRequest d) {
        if (d == null) throw new IllegalArgumentException("Informe o CNPJ da corretora");
        String cnpj = Cnpj.normalizar(d.cnpj());
        Usuario u = atual.bloquear();
        Corretora existente = repo.findByUsuarioIdAndCnpj(u.getId(), cnpj).orElse(null);
        if (existente != null && (!existente.isRemovida() || d.corretoraLegadaId() != null))
            throw new ConflitoException("Este CNPJ já está cadastrado na sua conta. Utilize a corretora existente.");
        Corretora legada = null;
        if (d.corretoraLegadaId() != null) {
            legada = propria(d.corretoraLegadaId(), u.getId());
            if (!legada.isLegada() || legada.getCnpj() != null)
                throw new IllegalArgumentException("A corretora já possui cadastro verificado");
        }
        CnpjResponse empresa = cadastro.consultar(cnpj);
        Corretora c;
        if (legada != null) {
            legada.regularizar(empresa.cnpj(), empresa.razaoSocial(), empresa.ativa(), empresa.consultadoEm());
            c = legada;
        } else if (existente != null) {
            existente.regularizar(empresa.cnpj(), empresa.razaoSocial(), empresa.ativa(), empresa.consultadoEm());
            c = existente;
        } else c = new Corretora(u, empresa.cnpj(), empresa.razaoSocial(), Mercado.BR, empresa.ativa(), empresa.consultadoEm());
        c.restaurar();
        return dto(repo.saveAndFlush(c));
    }
    @Transactional
    public void remover(Long id) {
        Usuario u = atual.bloquear();
        Corretora c = propria(id, u.getId());
        transacoes.deleteByUsuarioIdAndCorretoraId(u.getId(), c.getId());
        repo.delete(c);
        repo.flush();
    }
    @Transactional(readOnly=true)
    public Corretora obterParaOperacao(Long id, TipoTransacao tipo) {
        Corretora c = propria(id, atual.obter().getId());
        if (tipo == TipoTransacao.COMPRA && (c.isRemovida() || !c.isAtiva() || c.isLegada() || c.getCnpj() == null || c.getVerificadoEm() == null))
            throw new IllegalArgumentException("Cadastre ou regularize uma corretora ativa por CNPJ antes de comprar");
        return c;
    }
    private Corretora propria(Long id, Long usuarioId) {
        return repo.findByIdAndUsuarioId(id,usuarioId).orElseThrow(()->new IllegalArgumentException("Corretora inválida para esta conta"));
    }
    public static CorretoraResponse dto(Corretora c) {
        return new CorretoraResponse(c.getId(),c.getCodigo(),c.getNome(),c.isAtiva(),c.isLegada(),c.getCnpj(),c.getMercado(),c.getVerificadoEm(),c.isRemovida());
    }
}
