package com.curso.repositories;
import com.curso.domains.Consulta; import org.springframework.data.jpa.repository.JpaRepository; import java.util.List;
public interface ConsultaRepository extends JpaRepository<Consulta,Long>{
 @org.springframework.data.jpa.repository.Query("select c from Consulta c where c.usuario.id=:usuarioId order by c.consultadoEm desc, c.id desc")
 List<Consulta> findByUsuarioIdOrderByConsultadoEmDesc(@org.springframework.data.repository.query.Param("usuarioId") Long usuarioId);
}
