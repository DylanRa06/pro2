
package com.krakedev.patitas_rescate.repositories;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.krakedev.patitas_rescate.entidades.Usuario;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);

    boolean existsByUsername(String username);

}
