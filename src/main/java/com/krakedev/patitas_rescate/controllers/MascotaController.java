package com.krakedev.patitas_rescate.controllers;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.krakedev.patitas_rescate.entidades.Mascota;
import com.krakedev.patitas_rescate.services.MascotaService;

@RestController
@RequestMapping("/api/mascotas")
public class MascotaController {

    private final MascotaService mascotaService;

    public MascotaController(MascotaService mascotaService) {
        this.mascotaService = mascotaService;
    }

    @PostMapping
    public ResponseEntity<?> registrar(@RequestBody Mascota mascota) {

        try {
            Mascota nuevaMascota = mascotaService.registrar(mascota);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(nuevaMascota);

        } catch (IllegalArgumentException e) {

            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<Mascota>> listar() {

        return ResponseEntity.ok(mascotaService.listar());
    }
}
