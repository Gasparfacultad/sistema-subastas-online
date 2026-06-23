package com.utn.frvm.subastas.repositories;

import com.utn.frvm.subastas.entities.Subasta;
import com.utn.frvm.subastas.enums.EstadoSubasta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SubastaRepository extends JpaRepository<Subasta, Long> {

    List<Subasta> findByEstado(EstadoSubasta estado);

    List<Subasta> findByVendedorId(Long vendedorId);

    @Query("SELECT s FROM Subasta s WHERE s.estado = :estado AND s.fechaCierre <= :fechaCierre")
    List<Subasta> findByEstadoAndFechaCierreBeforeOrEqual(
            @Param("estado") EstadoSubasta estado,
            @Param("fechaCierre") LocalDateTime fechaCierre
    );
}
