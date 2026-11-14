package com.krakedev.patitas_rescate.repositories;

import org.springframework.data.jpa.repository.JpaRepository;

import com.krakedev.patitas_rescate.entidades.Mascota;

public interface MascotaRepository extends JpaRepository<Mascota, Long> {

}