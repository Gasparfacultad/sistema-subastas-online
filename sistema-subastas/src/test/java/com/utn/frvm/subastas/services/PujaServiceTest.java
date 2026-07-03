package com.utn.frvm.subastas.services;

import com.utn.frvm.subastas.dtos.PujaRequestDTO;
import com.utn.frvm.subastas.entities.Puja;
import com.utn.frvm.subastas.entities.Subasta;
import com.utn.frvm.subastas.entities.Usuario;
import com.utn.frvm.subastas.enums.EstadoSubasta;
import com.utn.frvm.subastas.enums.EstadoUsuario;
import com.utn.frvm.subastas.exceptions.BusinessRuleException;
import com.utn.frvm.subastas.exceptions.ResourceNotFoundException;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PujaServiceTest {

    @Mock
    private PujaRepository pujaRepository;

    @Mock
    private SubastaRepository subastaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private PujaService pujaService;

    private Usuario activo;
    private Usuario bloqueado;
    private Subasta subasta;

    @BeforeEach
    void setUp() {
        activo = Usuario.builder()
                .id(1L)
                .username("comprador")
                .estado(EstadoUsuario.ACTIVO)
                .build();

        bloqueado = Usuario.builder()
                .id(2L)
                .username("bloqueado")
                .estado(EstadoUsuario.BLOQUEADO)
                .build();

        subasta = Subasta.builder()
                .id(1L)
                .estado(EstadoSubasta.ACTIVA)
                .precioBase(BigDecimal.valueOf(100))
                .montoActual(BigDecimal.valueOf(100))
                .incrementoMinimoPuja(BigDecimal.TEN)
                .fechaInicio(LocalDateTime.now().minusDays(1))
                .fechaCierre(LocalDateTime.now().plusDays(1))
                .build();
    }

    @Test
    void shouldRejectBlockedUser() {
        PujaRequestDTO request = PujaRequestDTO.builder()
                .subastaId(subasta.getId())
                .compradorId(bloqueado.getId())
                .monto(BigDecimal.valueOf(115))
                .build();

        when(subastaRepository.findByIdForUpdate(eq(subasta.getId()))).thenReturn(Optional.of(subasta));
        when(usuarioRepository.findById(eq(bloqueado.getId()))).thenReturn(Optional.of(bloqueado));

        assertThrows(BusinessRuleException.class, () -> pujaService.realizarPuja(request, bloqueado.getId()));

        verify(pujaRepository, never()).save(any(Puja.class));
    }

    @Test
    void shouldRejectExpiredAuction() {
        subasta.setFechaCierre(LocalDateTime.now().minusHours(1));

        PujaRequestDTO request = PujaRequestDTO.builder()
                .subastaId(subasta.getId())
                .compradorId(activo.getId())
                .monto(BigDecimal.valueOf(115))
                .build();

        when(subastaRepository.findByIdForUpdate(eq(subasta.getId()))).thenReturn(Optional.of(subasta));
        when(usuarioRepository.findById(eq(activo.getId()))).thenReturn(Optional.of(activo));

        assertThrows(BusinessRuleException.class, () -> pujaService.realizarPuja(request, activo.getId()));

        verify(pujaRepository, never()).save(any(Puja.class));
    }
}
