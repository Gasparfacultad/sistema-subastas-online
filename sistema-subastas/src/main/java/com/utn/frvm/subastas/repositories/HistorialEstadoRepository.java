package com.utn.frvm.subastas.repositories;

import com.utn.frvm.subastas.entities.HistorialEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface HistorialEstadoRepository extends JpaRepository<HistorialEstado, Long> {
}
