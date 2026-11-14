package com.krakedev.patitas_rescate.repositories;

import java.util.List;
import java.util.Optional;
import com.krakedev.patitas_rescate.entidades.Mascota;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MascotaRepository extends JpaRepository<Mascota, Long> {
    List<Mascota> findByEstado(String estado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from Mascota m where m.id = :id")
    Optional<Mascota> buscarParaAdoptar(@Param("id") Long id);
}
