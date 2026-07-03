package com.utn.frvm.subastas.repositories;

import com.utn.frvm.subastas.entities.HistorialIncidencia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HistorialIncidenciaRepository extends JpaRepository<HistorialIncidencia, Long> {

    List<HistorialIncidencia> findByUsuarioId(Long usuarioId);
}
