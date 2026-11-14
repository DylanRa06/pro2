package com.krakedev.patitas_rescate.controllers;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasRole('ADMIN')")
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

    @GetMapping("/disponibles")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<Mascota>> disponibles() {
        return ResponseEntity.ok(mascotaService.listarDisponibles());
    }

    @PutMapping("/{id}/adoptar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Mascota> adoptar(@PathVariable Long id) {
        return ResponseEntity.ok(mascotaService.adoptar(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ResponseEntity<List<Mascota>> listar() {

        return ResponseEntity.ok(mascotaService.listar());
    }
}

