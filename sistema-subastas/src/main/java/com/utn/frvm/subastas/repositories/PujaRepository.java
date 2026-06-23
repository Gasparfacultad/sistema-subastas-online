package com.utn.frvm.subastas.repositories;

import com.utn.frvm.subastas.entities.Puja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PujaRepository extends JpaRepository<Puja, Long> {

    List<Puja> findBySubastaIdOrderByMontoDesc(Long subastaId);
}
