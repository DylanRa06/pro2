package com.krakedev.patitas_rescate.config;

import java.util.Date;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    private static final long EXPIRACION = 3600000;

    public String generarToken(String username, String rol) {

        Algorithm algoritmo = Algorithm.HMAC256(secret);

        return JWT.create()
                .withIssuer("patitas-rescate")
                .withSubject(username)
                .withClaim("rol", rol)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRACION))
                .sign(algoritmo);
    }
}