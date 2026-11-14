
package com.krakedev.patitas_rescate.services;

import java.util.List;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.stereotype.Service;

import com.krakedev.patitas_rescate.entidades.Mascota;
import com.krakedev.patitas_rescate.repositories.MascotaRepository;

@Service
public class MascotaService {

    private final MascotaRepository mascotaRepository;

    public MascotaService(MascotaRepository mascotaRepository) {
        this.mascotaRepository = mascotaRepository;
    }

    public Mascota registrar(Mascota mascota) {

        if (mascota == null) {
            throw new IllegalArgumentException("La mascota es obligatoria");
        }

        if (mascota.getNombre() == null || mascota.getNombre().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre es obligatorio");
        }

        if (mascota.getEspecie() == null || mascota.getEspecie().trim().isEmpty()) {
            throw new IllegalArgumentException("La especie es obligatoria");
        }

        if (mascota.getEdad() < 0) {
            throw new IllegalArgumentException("La edad no puede ser negativa");
        }

        mascota.setId(null);
        mascota.setNombre(mascota.getNombre().trim());
        mascota.setEspecie(mascota.getEspecie().trim());
        mascota.setEstado("DISPONIBLE");

        return mascotaRepository.save(mascota);
    }

    public List<Mascota> listarDisponibles() {
        return mascotaRepository.findByEstado("DISPONIBLE");
    }

    @Transactional
    public Mascota adoptar(Long id) {
        Mascota mascota = mascotaRepository.buscarParaAdoptar(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "La mascota no existe"));
        if (!"DISPONIBLE".equals(mascota.getEstado())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La mascota ya fue adoptada o no está disponible");
        }
        mascota.setEstado("ADOPTADA");
        return mascotaRepository.save(mascota);
    }

    public List<Mascota> listar() {
        return mascotaRepository.findAll();
    }
}

