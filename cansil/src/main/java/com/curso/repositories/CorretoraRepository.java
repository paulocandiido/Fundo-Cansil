package com.curso.repositories;
import com.curso.domains.Corretora; import java.util.*; import org.springframework.data.jpa.repository.JpaRepository;
public interface CorretoraRepository extends JpaRepository<Corretora,Long>{
 List<Corretora> findByUsuarioIdOrderByNomeAscIdAsc(Long usuarioId);
 Optional<Corretora> findByIdAndUsuarioId(Long id,Long usuarioId);
 boolean existsByUsuarioIdAndCnpj(Long usuarioId,String cnpj);
 Optional<Corretora> findByUsuarioIdAndCnpj(Long usuarioId,String cnpj);
 Optional<Corretora> findByCodigo(String codigo);
}
