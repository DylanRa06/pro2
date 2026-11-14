package com.krakedev.patitas_rescate.services;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.springframework.stereotype.Service;

@Service
public class TokenBlacklistService {
    private final ConcurrentHashMap<String, Instant> tokens = new ConcurrentHashMap<>();

    public boolean estaRevocado(String token) {
        Instant expiracion = tokens.get(token);
        if (expiracion == null) return false;
        if (!expiracion.isAfter(Instant.now())) {
            tokens.remove(token, expiracion);
            return false;
        }
        return true;
    }

    public void revocar(DecodedJWT token) {
        Instant ahora = Instant.now();
        tokens.entrySet().removeIf(entrada -> !entrada.getValue().isAfter(ahora));
        if (token.getExpiresAtAsInstant().isAfter(ahora)) {
            tokens.put(token.getToken(), token.getExpiresAtAsInstant());
        }
    }
}
