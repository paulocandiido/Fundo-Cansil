package com.curso.repositories;
import com.curso.domains.Usuario; import org.springframework.data.jpa.repository.JpaRepository; import java.util.Optional;
public interface UsuarioRepository extends JpaRepository<Usuario,Long>{ Optional<Usuario> findByEmailIgnoreCase(String email); boolean existsByEmailIgnoreCase(String email); boolean existsByCpf(String cpf);
 @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
 @org.springframework.data.jpa.repository.Query("select u from Usuario u where u.id=:id")
 Optional<Usuario> bloquear(@org.springframework.data.repository.query.Param("id") Long id);
}
