package com.utn.frvm.subastas.services;

import com.utn.frvm.subastas.dtos.ProductoRequestDTO;
import com.utn.frvm.subastas.dtos.SubastaRequestDTO;
import com.utn.frvm.subastas.entities.Categoria;
import com.utn.frvm.subastas.entities.Producto;
import com.utn.frvm.subastas.entities.Usuario;
import com.utn.frvm.subastas.enums.EstadoProducto;
import com.utn.frvm.subastas.enums.EstadoSubasta;
import com.utn.frvm.subastas.enums.EstadoUsuario;
import com.utn.frvm.subastas.exceptions.BusinessRuleException;
import com.utn.frvm.subastas.repositories.CategoriaRepository;
import com.utn.frvm.subastas.repositories.ProductoRepository;
import com.utn.frvm.subastas.repositories.SubastaRepository;
import com.utn.frvm.subastas.repositories.UsuarioRepository;
import com.utn.frvm.subastas.repositories.HistorialEstadoRepository;
import com.utn.frvm.subastas.repositories.PujaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductoEstadoTest {

    @Mock
    private ProductoRepository productoRepository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private SubastaRepository subastaRepository;

    @Mock
    private HistorialEstadoRepository historialEstadoRepository;

    @Mock
    private NotificacionService notificacionService;

    @Mock
    private PujaRepository pujaRepository;

    @InjectMocks
    private ProductoService productoService;

    private SubastaService subastaService;

    private Usuario vendedor;
    private Categoria categoria;
    private Producto producto;

    @BeforeEach
    void setUp() {
        // Initialize subastaService manually because it needs several mocked repositories and services
        subastaService = new SubastaService(
                subastaRepository,
                productoRepository,
                usuarioRepository,
                historialEstadoRepository,
                notificacionService,
                pujaRepository
        );

        vendedor = Usuario.builder()
                .id(1L)
                .username("vendedor")
                .estado(EstadoUsuario.ACTIVO)
                .build();

        categoria = Categoria.builder()
                .id(1L)
                .nombre("Electrónica")
                .build();

        producto = Producto.builder()
                .id(1L)
                .vendedor(vendedor)
                .categoria(categoria)
                .nombre("Televisor")
                .descripcion("Televisor 4K")
                .estado(EstadoProducto.ACTIVO)
                .build();
    }

    @Test
    void shouldForceActiveOnProductCreate() {
        ProductoRequestDTO request = ProductoRequestDTO.builder()
                .categoriaId(1L)
                .vendedorId(1L)
                .nombre("Televisor")
                .descripcion("Televisor 4K")
                .estado(EstadoProducto.INACTIVO) // Client passes INACTIVO
                .build();

        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(vendedor));
        when(productoRepository.save(any(Producto.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = productoService.create(request);

        assertNotNull(response);
        assertEquals(EstadoProducto.ACTIVO, response.getEstado()); // Should be forced to ACTIVO
        verify(productoRepository).save(argThat(p -> p.getEstado() == EstadoProducto.ACTIVO));
    }

    @Test
    void shouldRejectUpdateToActiveIfProductBelongsToSubasta() {
        ProductoRequestDTO request = ProductoRequestDTO.builder()
                .categoriaId(1L)
                .vendedorId(1L)
                .nombre("Televisor")
                .descripcion("Televisor 4K")
                .estado(EstadoProducto.ACTIVO) // Attempting to set to ACTIVO
                .build();

        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(categoriaRepository.findById(1L)).thenReturn(Optional.of(categoria));
        when(subastaRepository.existsByProductoId(1L)).thenReturn(true); // Belongs to a subasta

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                productoService.update(1L, request, 1L)
        );

        assertEquals("No se puede activar un producto que ya pertenece a una subasta", ex.getMessage());
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        verify(productoRepository, never()).save(any(Producto.class));
    }

    @Test
    void shouldDeactivateProductOnSubastaCreate() {
        SubastaRequestDTO request = SubastaRequestDTO.builder()
                .vendedorId(1L)
                .productoId(1L)
                .precioBase(BigDecimal.valueOf(100))
                .incrementoMinimoPuja(BigDecimal.TEN)
                .titulo("Subasta TV")
                .fechaInicio(LocalDateTime.now().plusDays(1))
                .fechaCierre(LocalDateTime.now().plusDays(2))
                .estado(EstadoSubasta.BORRADOR)
                .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(vendedor));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(subastaRepository.existsByProductoId(1L)).thenReturn(false); // Does not belong to subasta yet
        when(subastaRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = subastaService.create(request);

        assertNotNull(response);
        assertEquals(EstadoProducto.INACTIVO, producto.getEstado()); // Product state should be updated to INACTIVO
        verify(productoRepository).save(producto);
        verify(subastaRepository).save(any());
    }

    @Test
    void shouldRejectSubastaCreateIfProductIsAlreadyInactive() {
        producto.setEstado(EstadoProducto.INACTIVO);

        SubastaRequestDTO request = SubastaRequestDTO.builder()
                .vendedorId(1L)
                .productoId(1L)
                .precioBase(BigDecimal.valueOf(100))
                .incrementoMinimoPuja(BigDecimal.TEN)
                .titulo("Subasta TV")
                .fechaInicio(LocalDateTime.now().plusDays(1))
                .fechaCierre(LocalDateTime.now().plusDays(2))
                .estado(EstadoSubasta.BORRADOR)
                .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(vendedor));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));

        BusinessRuleException ex = assertThrows(BusinessRuleException.class, () ->
                subastaService.create(request)
        );

        assertEquals("El producto no está activo o disponible para subasta.", ex.getMessage());
        verify(subastaRepository, never()).save(any());
    }
}
