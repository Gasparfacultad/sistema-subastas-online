package com.utn.frvm.subastas.services;

import com.utn.frvm.subastas.dtos.DisputaRequestDTO;
import com.utn.frvm.subastas.dtos.DisputaResponseDTO;
import com.utn.frvm.subastas.dtos.HistorialIncidenciaResponseDTO;
import com.utn.frvm.subastas.entities.HistorialIncidencia;
import com.utn.frvm.subastas.entities.Disputa;
import com.utn.frvm.subastas.entities.HistorialEstado;
import com.utn.frvm.subastas.entities.Subasta;
import com.utn.frvm.subastas.entities.Usuario;
import com.utn.frvm.subastas.enums.EstadoDisputa;
import com.utn.frvm.subastas.enums.EstadoSubasta;
import com.utn.frvm.subastas.enums.EstadoUsuario;
import com.utn.frvm.subastas.enums.RolUsuario;
import com.utn.frvm.subastas.exceptions.BusinessRuleException;
import com.utn.frvm.subastas.exceptions.ResourceNotFoundException;
import com.utn.frvm.subastas.enums.EstadoProducto;
import com.utn.frvm.subastas.repositories.DisputaRepository;
import com.utn.frvm.subastas.repositories.HistorialEstadoRepository;
import com.utn.frvm.subastas.repositories.HistorialIncidenciaRepository;
import com.utn.frvm.subastas.repositories.ProductoRepository;
import com.utn.frvm.subastas.repositories.SubastaRepository;
import com.utn.frvm.subastas.repositories.UsuarioRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DisputaService {

    private final HistorialEstadoRepository historialEstadoRepository;
    private final DisputaRepository disputaRepository;
    private final SubastaRepository subastaRepository;
    private final UsuarioRepository usuarioRepository;
    private final HistorialIncidenciaRepository historialIncidenciaRepository;
    private final ProductoRepository productoRepository;

    public DisputaService(DisputaRepository disputaRepository, SubastaRepository subastaRepository,
            UsuarioRepository usuarioRepository, HistorialIncidenciaRepository historialIncidenciaRepository,
            HistorialEstadoRepository historialEstadoRepository, ProductoRepository productoRepository) {
        this.disputaRepository = disputaRepository;
        this.subastaRepository = subastaRepository;
        this.usuarioRepository = usuarioRepository;
        this.historialIncidenciaRepository = historialIncidenciaRepository;
        this.historialEstadoRepository = historialEstadoRepository;
        this.productoRepository = productoRepository;
    }

    @Transactional
    public DisputaResponseDTO openDispute(DisputaRequestDTO request) {
        Subasta subasta = subastaRepository.findById(request.getSubastaId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Subasta no encontrada con ID: " + request.getSubastaId()));

        Usuario iniciador = usuarioRepository.findById(request.getIniciadorId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Usuario iniciador no encontrado con ID: " + request.getIniciadorId()));

        boolean isSeller = subasta.getVendedor().getId().equals(iniciador.getId());
        boolean isWinner = subasta.getGanador() != null && subasta.getGanador().getId().equals(iniciador.getId());

        if (!isSeller && !isWinner) {
            throw new BusinessRuleException("Solo el comprador ganador o el vendedor pueden iniciar una disputa.");
        }
        if (iniciador.getEstado() != EstadoUsuario.ACTIVO) {
            throw new BusinessRuleException("Un usuario bloqueado o inactivo no puede abrir disputas.",
                    HttpStatus.FORBIDDEN);
        }
        if (subasta.getEstado() == EstadoSubasta.EN_DISPUTA) {
            throw new BusinessRuleException("La subasta ya se encuentra en disputa.");
        }

        if (subasta.getEstado() != EstadoSubasta.ADJUDICADA && subasta.getEstado() != EstadoSubasta.FINALIZADA) {
            throw new BusinessRuleException("Solo se pueden iniciar disputas en subastas finalizadas o adjudicadas.");
        }

        subasta.setEstado(EstadoSubasta.EN_DISPUTA);
        if (subasta.getProducto() != null) {
            subasta.getProducto().setEstado(EstadoProducto.INACTIVO);
            productoRepository.save(subasta.getProducto());
        }
        subastaRepository.save(subasta);

        Disputa disputa = Disputa.builder()
                .subasta(subasta)
                .iniciador(iniciador)
                .motivoApertura(request.getMotivoApertura())
                .estado(EstadoDisputa.ABIERTA)
                .build();

        return mapToResponse(disputaRepository.save(disputa));
    }

    @Transactional
    public DisputaResponseDTO resolverDisputa(Long disputaId, EstadoDisputa resolucion, Long adminId) {
        Disputa disputa = disputaRepository.findById(disputaId)
                .orElseThrow(() -> new ResourceNotFoundException("Disputa no encontrada con ID: " + disputaId));

        Usuario admin = usuarioRepository.findById(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("Administrador no encontrado con ID: " + adminId));

        if (admin.getRol() != RolUsuario.ROLE_ADMIN) {
            throw new BusinessRuleException("Solo un administrador puede resolver disputas.");
        }

        if (disputa.getEstado() != EstadoDisputa.ABIERTA) {
            throw new BusinessRuleException("La disputa ya ha sido resuelta o no está abierta.");
        }

        if (resolucion != EstadoDisputa.RESUELTA_FAVOR_USER && resolucion != EstadoDisputa.RESUELTA_FAVOR_SELLER) {
            throw new BusinessRuleException("Estado de resolución no válido.");
        }

        disputa.setEstado(resolucion);
        disputa.setAdminResolutor(admin);
        disputa.setJustificacionResolucion("Resolución administrativa");
        disputa.setFechaResolucion(LocalDateTime.now());

        Subasta subasta = disputa.getSubasta();
        EstadoSubasta estadoAnterior = subasta.getEstado();
        
        if (resolucion == EstadoDisputa.RESUELTA_FAVOR_USER) {
            subasta.setEstado(EstadoSubasta.CANCELADA);
        } else {
            subasta.setEstado(EstadoSubasta.ADJUDICADA);
        }
        if (subasta.getProducto() != null) {
            subasta.getProducto().setEstado(EstadoProducto.INACTIVO);
            productoRepository.save(subasta.getProducto());
        }
        subastaRepository.save(subasta);

        HistorialEstado historial = HistorialEstado.builder()
            .subasta(subasta)
            .usuarioResponsable(admin)
            .estadoAnterior(estadoAnterior)
            .estadoNuevo(subasta.getEstado())
            .motivo("Resolución administrativa de disputa #" + disputa.getId())
            .build();
        historialEstadoRepository.save(historial);

        Usuario perdedor;
        if (resolucion == EstadoDisputa.RESUELTA_FAVOR_USER) {
            perdedor = subasta.getVendedor();
        } else {
            // Protección: si la subasta no tiene ganador asignado, no acumular incidencia
            if (subasta.getGanador() == null) {
                return mapToResponse(disputaRepository.save(disputa));
            }
            perdedor = subasta.getGanador();
        }
        perdedor.registrarIncidencia(admin, "Disputa resuelta en su contra.");
        usuarioRepository.save(perdedor);
        HistorialIncidencia incidencia = HistorialIncidencia.builder()
                .usuario(perdedor)
                .disputa(disputa)
                .motivoPenalizacion("Disputa resuelta en su contra.")
                .build();
        historialIncidenciaRepository.save(incidencia);
        return mapToResponse(disputaRepository.save(disputa));
    }

    @Transactional
    public DisputaResponseDTO resolveDispute(Long id, Long adminId, String justificacion, EstadoDisputa nuevoEstado) {
        DisputaResponseDTO dto = resolverDisputa(id, nuevoEstado, adminId);

        Disputa disputa = disputaRepository.findById(id).orElseThrow();
        disputa.setJustificacionResolucion(justificacion);
        disputaRepository.save(disputa);

        return mapToResponse(disputa);
    }

    @Transactional(readOnly = true)
    public DisputaResponseDTO getById(Long id) {
        Disputa disputa = disputaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Disputa no encontrada con ID: " + id));
        return mapToResponse(disputa);
    }

    @Transactional(readOnly = true)
    public List<DisputaResponseDTO> getDisputesByEstado(EstadoDisputa estado) {
        return disputaRepository.findByEstado(estado).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<DisputaResponseDTO> getDisputesByAdmin(Long adminId) {
        return disputaRepository.findByAdminResolutorId(adminId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<HistorialIncidenciaResponseDTO> getIncidenciasByUsuario(Long usuarioId) {
        return historialIncidenciaRepository.findByUsuarioId(usuarioId)
                .stream()
                .map(this::mapIncidenciaToResponse)
                .collect(Collectors.toList());
    }

    private DisputaResponseDTO mapToResponse(Disputa disputa) {
        return DisputaResponseDTO.builder()
                .id(disputa.getId())
                .subastaId(disputa.getSubasta().getId())
                .iniciadorId(disputa.getIniciador().getId())
                .iniciadorUsername(disputa.getIniciador().getUsername())
                .adminResolutorId(disputa.getAdminResolutor() != null ? disputa.getAdminResolutor().getId() : null)
                .adminResolutorUsername(
                        disputa.getAdminResolutor() != null ? disputa.getAdminResolutor().getUsername() : null)
                .motivoApertura(disputa.getMotivoApertura())
                .justificacionResolucion(disputa.getJustificacionResolucion())
                .estado(disputa.getEstado())
                .fechaApertura(disputa.getFechaApertura())
                .fechaResolucion(disputa.getFechaResolucion())
                .build();
    }

    private HistorialIncidenciaResponseDTO mapIncidenciaToResponse(HistorialIncidencia incidencia) {
        return HistorialIncidenciaResponseDTO.builder()
                .id(incidencia.getId())
                .usuarioId(incidencia.getUsuario().getId())
                .usuarioUsername(incidencia.getUsuario().getUsername())
                .disputaId(incidencia.getDisputa().getId())
                .motivoPenalizacion(incidencia.getMotivoPenalizacion())
                .fechaRegistro(incidencia.getFechaRegistro())
                .build();
    }
}
