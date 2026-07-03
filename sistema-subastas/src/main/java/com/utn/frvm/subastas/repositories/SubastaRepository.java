package com.utn.frvm.subastas.repositories;

import com.utn.frvm.subastas.entities.Subasta;
import com.utn.frvm.subastas.enums.EstadoSubasta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import java.util.Optional;
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
            @Param("fechaCierre") LocalDateTime fechaCierre);

    @Query("SELECT s FROM Subasta s WHERE s.estado = :estado AND s.fechaInicio <= :ahora")
    List<Subasta> findByEstadoAndFechaInicioBeforeOrEqual(
        @Param("estado") EstadoSubasta estado,
        @Param("ahora") LocalDateTime ahora
    );        
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Subasta s WHERE s.id = :id")
    Optional<Subasta> findByIdForUpdate(@Param("id") Long id);

    long countByProductoIdAndEstadoIn(Long id, List<EstadoSubasta> asList);

    boolean existsByProductoId(Long productoId);
}
