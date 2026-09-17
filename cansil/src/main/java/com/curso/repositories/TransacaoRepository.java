package com.curso.repositories;
import com.curso.domains.Transacao; import jakarta.persistence.LockModeType; import org.springframework.data.jpa.repository.*; import org.springframework.data.repository.query.Param; import java.util.List;
public interface TransacaoRepository extends JpaRepository<Transacao,Long>{
 @EntityGraph(attributePaths={"ativo","corretora"})
 List<Transacao> findByUsuarioIdOrderByDataOperacaoAscIdAsc(Long usuarioId);
 List<Transacao> findByUsuarioIdAndAtivoIdOrderByDataOperacaoAscIdAsc(Long usuarioId,Long ativoId);
 @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select t from Transacao t where t.usuario.id=:usuarioId and t.ativo.id=:ativoId order by t.dataOperacao,t.id")
 List<Transacao> findForUpdate(@Param("usuarioId") Long usuarioId,@Param("ativoId") Long ativoId);
 @Modifying(flushAutomatically=true,clearAutomatically=true)
 @Query("delete from Transacao t where t.usuario.id=:usuarioId and t.corretora.id=:corretoraId")
 int deleteByUsuarioIdAndCorretoraId(@Param("usuarioId") Long usuarioId,@Param("corretoraId") Long corretoraId);
}
