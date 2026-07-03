package com.utn.frvm.subastas.repositories;

import com.utn.frvm.subastas.entities.Producto;
import com.utn.frvm.subastas.enums.EstadoProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ProductoRepository extends JpaRepository<Producto, Long> {

    List<Producto> findByVendedorId(Long vendedorId);

    List<Producto> findByEstado(EstadoProducto estado);
}
