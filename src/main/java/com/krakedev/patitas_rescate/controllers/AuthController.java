package com.krakedev.patitas_rescate.controllers;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.krakedev.patitas_rescate.config.JwtUtil;
import com.krakedev.patitas_rescate.entidades.Usuario;
import com.krakedev.patitas_rescate.services.UsuarioService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioService usuarioService;
    private final JwtUtil jwtUtil;

    public AuthController(UsuarioService usuarioService, JwtUtil jwtUtil) {
        this.usuarioService = usuarioService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/registrar")
    public ResponseEntity<?> registrar(@RequestBody Usuario usuario) {

        try {
            Usuario nuevoUsuario = usuarioService.registrar(usuario);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "mensaje", "Usuario registrado correctamente",
                    "id", nuevoUsuario.getId(),
                    "username", nuevoUsuario.getUsername(),
                    "rol", nuevoUsuario.getRol()
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> datos) {

        try {
            String username = datos.get("username");
            String password = datos.get("password");

            if (username == null || username.isBlank()
                    || password == null || password.isBlank()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Username y contraseña son obligatorios"));
            }

            Usuario usuario = usuarioService.autenticar(username.trim(), password);

            String token = jwtUtil.generarToken(
                    usuario.getUsername(),
                    usuario.getRol()
            );

            return ResponseEntity.ok(Map.of(
                    "mensaje", "Inicio de sesión exitoso",
                    "token", token,
                    "tipo", "Bearer"
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Credenciales incorrectas"));
        }
    }
}