package com.utn.frvm.subastas.services;

import com.utn.frvm.subastas.dtos.ProductoRequestDTO;
import com.utn.frvm.subastas.dtos.ProductoResponseDTO;
import com.utn.frvm.subastas.entities.Categoria;
import com.utn.frvm.subastas.entities.Producto;
import com.utn.frvm.subastas.entities.Usuario;
import com.utn.frvm.subastas.enums.EstadoProducto;
import com.utn.frvm.subastas.enums.EstadoSubasta;
import com.utn.frvm.subastas.exceptions.BusinessRuleException;
import com.utn.frvm.subastas.exceptions.ResourceNotFoundException;
import com.utn.frvm.subastas.repositories.CategoriaRepository;
import com.utn.frvm.subastas.repositories.ProductoRepository;
import com.utn.frvm.subastas.repositories.SubastaRepository;
import com.utn.frvm.subastas.repositories.UsuarioRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductoService {

    private final SubastaRepository subastaRepository;
    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;

    public ProductoService(ProductoRepository productoRepository, CategoriaRepository categoriaRepository, UsuarioRepository usuarioRepository, SubastaRepository subastaRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.subastaRepository = subastaRepository;
    }

    @Transactional
    public ProductoResponseDTO create(ProductoRequestDTO request) {
        Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + request.getCategoriaId()));
        Usuario vendedor = usuarioRepository.findById(request.getVendedorId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendedor no encontrado con ID: " + request.getVendedorId()));

        Producto producto = Producto.builder()
                .categoria(categoria)
                .vendedor(vendedor)
                .nombre(request.getNombre())
                .descripcion(request.getDescripcion())
                .estado(request.getEstado())
                .build();

        return mapToResponse(productoRepository.save(producto));
    }

    @Transactional(readOnly = true)
    public ProductoResponseDTO getById(Long id) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));
        return mapToResponse(producto);
    }

    @Transactional(readOnly = true)
    public List<ProductoResponseDTO> getByVendedorId(Long vendedorId) {
        return productoRepository.findByVendedorId(vendedorId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ProductoResponseDTO> getByEstado(EstadoProducto estado) {
        return productoRepository.findByEstado(estado).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void update(Long id, ProductoRequestDTO request, Long usuarioId) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));
        Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + request.getCategoriaId()));

        // VALIDAR QUE EL USUARIO ES EL VENDEDOR
        if (!producto.getVendedor().getId().equals(usuarioId)) {
                throw new BusinessRuleException(
                "Solo el vendedor puede actualizar una subasta", 
                HttpStatus.FORBIDDEN
            );
        }

        boolean tieneSubastaActiva = producto.getSubastas().stream()
            .anyMatch(subasta -> subasta.getEstado() == EstadoSubasta.ACTIVA);
        if (tieneSubastaActiva) {
                throw new BusinessRuleException(
                "No se puede modificar un producto que está en una subasta activa"
                );
        }
        producto.setCategoria(categoria);
        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        producto.setEstado(request.getEstado());
        productoRepository.save(producto);
    }


    @Transactional
        public void delete(Long id, Long usuarioId) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));
        
        // Solo el vendedor propietario puede eliminar
        if (!producto.getVendedor().getId().equals(usuarioId)) {
                throw new BusinessRuleException(
                "Solo el vendedor puede eliminar un producto", 
                HttpStatus.FORBIDDEN
                );
        }
        
        // No puede haber subastas activas del producto
        long subastasActivas = subastaRepository.countByProductoIdAndEstadoIn(  id, Arrays.asList(EstadoSubasta.ACTIVA, EstadoSubasta.PUBLICADA, EstadoSubasta.BORRADOR)
        );
        
        if (subastasActivas > 0) {
                throw new BusinessRuleException(
                "No se puede eliminar un producto con subastas activas o pendientes"
                );
        }
        
        productoRepository.deleteById(id);
        }
    private ProductoResponseDTO mapToResponse(Producto producto) {
        return ProductoResponseDTO.builder()
                .id(producto.getId())
                .categoriaId(producto.getCategoria().getId())
                .categoriaNombre(producto.getCategoria().getNombre())
                .vendedorId(producto.getVendedor().getId())
                .vendedorUsername(producto.getVendedor().getUsername())
                .nombre(producto.getNombre())
                .descripcion(producto.getDescripcion())
                .estado(producto.getEstado())
                .creadoEn(producto.getCreadoEn())
                .actualizadoEn(producto.getActualizadoEn())
                .build();
    }
}
