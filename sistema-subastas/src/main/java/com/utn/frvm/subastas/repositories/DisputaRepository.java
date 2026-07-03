package com.utn.frvm.subastas.repositories;

import com.utn.frvm.subastas.entities.Disputa;
import com.utn.frvm.subastas.enums.EstadoDisputa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface DisputaRepository extends JpaRepository<Disputa, Long> {

    List<Disputa> findByAdminResolutorId(Long adminResolutorId);

    List<Disputa> findByEstado(EstadoDisputa estado);
}
