package com.utn.frvm.subastas.services;

import com.utn.frvm.subastas.entities.Subasta;
import com.utn.frvm.subastas.entities.Usuario;
import com.utn.frvm.subastas.enums.EstadoSubasta;
import com.utn.frvm.subastas.repositories.HistorialEstadoRepository;
import com.utn.frvm.subastas.repositories.PujaRepository;
import com.utn.frvm.subastas.repositories.SubastaRepository;
import com.utn.frvm.subastas.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubastaSchedulerTest {

    @Mock private SubastaRepository subastaRepository;
    @Mock private HistorialEstadoRepository historialEstadoRepository;
    @Mock private NotificacionService notificacionService;
    @Mock private PujaRepository pujaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private com.utn.frvm.subastas.repositories.ProductoRepository productoRepository;

    @InjectMocks
    private SubastaService subastaService;

    private Subasta subastaActiva;
    private Subasta subastaPublicada;
    private Usuario vendedor;

    @BeforeEach
    void setUp() {
        vendedor = Usuario.builder()
                .id(1L)
                .username("vendedor1")
                .build();

        subastaActiva = Subasta.builder()
                .id(10L)
                .titulo("Subasta Activa")
                .estado(EstadoSubasta.ACTIVA)
                .vendedor(vendedor)
                .montoActual(BigDecimal.valueOf(100))
                .fechaCierre(LocalDateTime.now().minusMinutes(1)) // ya expirada
                .build();

        subastaPublicada = Subasta.builder()
                .id(20L)
                .titulo("Subasta Publicada")
                .estado(EstadoSubasta.PUBLICADA)
                .vendedor(vendedor)
                .fechaInicio(LocalDateTime.now().minusSeconds(10)) // ya debería activarse
                .build();
    }

    @Test
    void cerrarSubastasExpiradas_debeFinalizarSubastaActivaVencida() {
        // Arrange: el repo devuelve la subasta activa vencida
        when(subastaRepository.findByEstadoAndFechaCierreBeforeOrEqual(
                eq(EstadoSubasta.ACTIVA), any(LocalDateTime.class)))
                .thenReturn(List.of(subastaActiva));

        // Act
        subastaService.cerrarSubastasExpiradas();

        // Assert: el estado se cambió a FINALIZADA y se guardó historial
        verify(subastaRepository).save(subastaActiva);
        verify(historialEstadoRepository).save(any());
        verify(notificacionService).sendNotification(
                eq(vendedor.getId()), eq(subastaActiva.getId()), any(), any());
    }

    @Test
    void activarSubastasDebidas_debeActivarSubastaPublicadaConFechaInicioPasada() {
        // Arrange
        when(subastaRepository.findByEstadoAndFechaInicioBeforeOrEqual(
                eq(EstadoSubasta.PUBLICADA), any(LocalDateTime.class)))
                .thenReturn(List.of(subastaPublicada));

        // Act
        subastaService.activarSubastasDebidas();

        // Assert
        verify(subastaRepository).save(subastaPublicada);
        verify(historialEstadoRepository).save(any());
        verify(notificacionService).sendNotification(
                eq(vendedor.getId()), eq(subastaPublicada.getId()), any(), any());
    }

    @Test
    void cerrarSubastasExpiradas_conGanador_debeAdjudicarGanador() {
        // Arrange: la subasta tiene ganador actual
        Usuario ganador = Usuario.builder().id(2L).username("comprador1").build();
        subastaActiva.setGanadorActual(ganador);

        when(subastaRepository.findByEstadoAndFechaCierreBeforeOrEqual(
                eq(EstadoSubasta.ACTIVA), any(LocalDateTime.class)))
                .thenReturn(List.of(subastaActiva));

        // Act
        subastaService.cerrarSubastasExpiradas();

        // Assert: ganador fue asignado
        verify(subastaRepository).save(subastaActiva);
        verify(historialEstadoRepository).save(any());
    }
}
