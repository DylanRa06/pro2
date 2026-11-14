package com.krakedev.patitas_rescate.config;

import com.krakedev.patitas_rescate.services.TokenBlacklistService;

import java.io.IOException;
import java.util.List;
import com.auth0.jwt.exceptions.JWTVerificationException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwt;
    private final TokenBlacklistService blacklist;
    public JwtAuthenticationFilter(JwtUtil jwt, TokenBlacklistService blacklist) {
        this.jwt = jwt;
        this.blacklist = blacklist;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "POST".equals(request.getMethod()) &&
                ("/api/auth/login".equals(request.getServletPath()) ||
                 "/api/auth/registrar".equals(request.getServletPath()));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null) {
            if (!header.regionMatches(true, 0, "Bearer ", 0, 7) || header.substring(7).isBlank()) {
                rechazar(response);
                return;
            }
            try {
                var token = jwt.validarToken(header.substring(7));
                if (blacklist.estaRevocado(token.getToken())) {
                    rechazar(response);
                    return;
                }
                var auth = new UsernamePasswordAuthenticationToken(token.getSubject(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + token.getClaim("rol").asString())));
                SecurityContextHolder.getContext().setAuthentication(auth);
                request.setAttribute("jwtValidado", token);
            } catch (JWTVerificationException | IllegalArgumentException error) {
                SecurityContextHolder.clearContext();
                rechazar(response);
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private void rechazar(HttpServletResponse response) throws IOException {
        response.setStatus(401);
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"mensaje\":\"Token inválido, expirado o revocado\"}");
    }
}
