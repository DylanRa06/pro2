package com.krakedev.patitas_rescate.config;

import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

@Component
public class JwtUtil {
    private final Algorithm algoritmo;
    private final long minutos;
    public JwtUtil(@Value("${app.jwt.secret:}") String secreto,
            @Value("${app.jwt.expiration-minutes:60}") long minutos) {
        if (secreto.isBlank()) {
            byte[] clave = new byte[32];
            new SecureRandom().nextBytes(clave);
            this.algoritmo = Algorithm.HMAC256(clave);
        } else {
            if (secreto.getBytes(StandardCharsets.UTF_8).length < 32) {
                throw new IllegalArgumentException("JWT_SECRET debe tener al menos 32 bytes");
            }
            this.algoritmo = Algorithm.HMAC256(secreto);
        }
        this.minutos = minutos;
    }

    public com.auth0.jwt.interfaces.DecodedJWT validarToken(String token) {
        var datos = JWT.require(algoritmo).withIssuer("patitas-rescate").build().verify(token);
        String rol = datos.getClaim("rol").asString();
        if (datos.getSubject() == null || datos.getSubject().isBlank()
                || datos.getId() == null || datos.getId().isBlank()
                || datos.getExpiresAtAsInstant() == null
                || !("USER".equals(rol) || "ADMIN".equals(rol))) {
            throw new com.auth0.jwt.exceptions.JWTVerificationException("Token inválido");
        }
        return datos;
    }

    public String generarToken(String username, String rol) {
        Instant ahora = Instant.now();
        return JWT.create().withIssuer("patitas-rescate")
                .withSubject(username)
                .withClaim("rol", rol)
                .withJWTId(UUID.randomUUID().toString())
                .withIssuedAt(ahora)
                .withExpiresAt(ahora.plus(minutos, ChronoUnit.MINUTES))
                .sign(algoritmo);
    }
}
