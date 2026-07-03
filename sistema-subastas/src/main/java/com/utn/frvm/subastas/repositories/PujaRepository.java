package com.utn.frvm.subastas.repositories;

import com.utn.frvm.subastas.entities.Puja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PujaRepository extends JpaRepository<Puja, Long> {

    List<Puja> findBySubastaIdOrderByMontoDesc(Long subastaId);

    long countBySubastaId(Long subastaId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Puja p SET p.esGanadora = false WHERE p.subasta.id = :subastaId AND p.id != :pujaGanadoraId")
    int updatePreviousWinningBidsToFalse(@Param("subastaId") Long subastaId,
                                         @Param("pujaGanadoraId") Long pujaGanadoraId);
}
