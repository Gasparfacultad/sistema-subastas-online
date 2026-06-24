package com.utn.frvm.subastas.services;

import com.utn.frvm.subastas.dtos.ProductoRequestDTO;
import com.utn.frvm.subastas.dtos.ProductoResponseDTO;
import com.utn.frvm.subastas.entities.Categoria;
import com.utn.frvm.subastas.entities.Producto;
import com.utn.frvm.subastas.entities.Usuario;
import com.utn.frvm.subastas.enums.EstadoProducto;
import com.utn.frvm.subastas.exceptions.ResourceNotFoundException;
import com.utn.frvm.subastas.repositories.CategoriaRepository;
import com.utn.frvm.subastas.repositories.ProductoRepository;
import com.utn.frvm.subastas.repositories.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;

    public ProductoService(ProductoRepository productoRepository, CategoriaRepository categoriaRepository, UsuarioRepository usuarioRepository) {
        this.productoRepository = productoRepository;
        this.categoriaRepository = categoriaRepository;
        this.usuarioRepository = usuarioRepository;
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
    public void update(Long id, ProductoRequestDTO request) {
        Producto producto = productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + id));
        Categoria categoria = categoriaRepository.findById(request.getCategoriaId())
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con ID: " + request.getCategoriaId()));

        producto.setCategoria(categoria);
        producto.setNombre(request.getNombre());
        producto.setDescripcion(request.getDescripcion());
        producto.setEstado(request.getEstado());
        productoRepository.save(producto);
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
