package com.krakedev.patitas_rescate.controllers;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> manejar(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode())
                .body(Map.of("mensaje", error.getReason() == null ? "Solicitud no válida" : error.getReason()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> jsonInvalido(HttpMessageNotReadableException error) {
        return ResponseEntity.badRequest().body(Map.of("mensaje", "El cuerpo JSON no es válido"));
    }
}
