package com.krakedev.patitas_rescate.services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.krakedev.patitas_rescate.entidades.Usuario;
import com.krakedev.patitas_rescate.repositories.UsuarioRepository;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public Usuario registrar(Usuario usuario) {

        if (usuario == null) {
            throw new IllegalArgumentException("Los datos son obligatorios");
        }

        if (usuario.getUsername() == null || usuario.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("El username es obligatorio");
        }

        if (usuario.getPassword() == null || usuario.getPassword().isBlank()) {
            throw new IllegalArgumentException("La contraseña es obligatoria");
        }

        usuario.setUsername(usuario.getUsername().trim());

        if (usuarioRepository.existsByUsername(usuario.getUsername())) {
            throw new IllegalArgumentException("El username ya está registrado");
        }

        usuario.setPassword(passwordEncoder.encode(usuario.getPassword()));
        usuario.setRol("USER");

        return usuarioRepository.save(usuario);
    }

    public Usuario autenticar(String username, String password) {

        Usuario usuario = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Credenciales incorrectas"));

        if (!passwordEncoder.matches(password, usuario.getPassword())) {
            throw new IllegalArgumentException("Credenciales incorrectas");
        }

        return usuario;
    }
}