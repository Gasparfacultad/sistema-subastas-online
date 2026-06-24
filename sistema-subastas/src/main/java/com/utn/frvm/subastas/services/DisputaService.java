package com.utn.frvm.subastas.services;

import com.utn.frvm.subastas.dtos.DisputaRequestDTO;
import com.utn.frvm.subastas.dtos.DisputaResponseDTO;
import com.utn.frvm.subastas.entities.Disputa;
import com.utn.frvm.subastas.entities.HistorialIncidencia;
import com.utn.frvm.subastas.entities.Subasta;
import com.utn.frvm.subastas.entities.Usuario;
import com.utn.frvm.subastas.enums.EstadoDisputa;
import com.utn.frvm.subastas.enums.EstadoSubasta;
import com.utn.frvm.subastas.enums.EstadoUsuario;
import com.utn.frvm.subastas.enums.RolUsuario;
import com.utn.frvm.subastas.exceptions.BusinessRuleException;
import com.utn.frvm.subastas.exceptions.ResourceNotFoundException;
import com.utn.frvm.subastas.repositories.DisputaRepository;
import com.utn.frvm.subastas.repositories.HistorialIncidenciaRepository;
import com.utn.frvm.subastas.repositories.SubastaRepository;
import com.utn.frvm.subastas.repositories.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DisputaService {

    private final DisputaRepository disputaRepository;
    private final SubastaRepository subastaRepository;
    private final UsuarioRepository usuarioRepository;
    private final HistorialIncidenciaRepository historialIncidenciaRepository;

    public DisputaService(DisputaRepository disputaRepository, SubastaRepository subastaRepository,
                          UsuarioRepository usuarioRepository, HistorialIncidenciaRepository historialIncidenciaRepository) {
        this.disputaRepository = disputaRepository;
        this.subastaRepository = subastaRepository;
        this.usuarioRepository = usuarioRepository;
        this.historialIncidenciaRepository = historialIncidenciaRepository;
    }

    @Transactional
    public DisputaResponseDTO openDispute(DisputaRequestDTO request) {
        Subasta subasta = subastaRepository.findById(request.getSubastaId())
                .orElseThrow(() -> new ResourceNotFoundException("Subasta no encontrada con ID: " + request.getSubastaId()));

        Usuario iniciador = usuarioRepository.findById(request.getIniciadorId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario iniciador no encontrado con ID: " + request.getIniciadorId()));

        boolean isSeller = subasta.getVendedor().getId().equals(iniciador.getId());
        boolean isWinner = subasta.getGanador() != null && subasta.getGanador().getId().equals(iniciador.getId());

        if (!isSeller && !isWinner) {
            throw new BusinessRuleException("Solo el comprador ganador o el vendedor pueden iniciar una disputa.");
        }

        if (subasta.getEstado() == EstadoSubasta.EN_DISPUTA) {
            throw new BusinessRuleException("La subasta ya se encuentra en disputa.");
        }

        if (subasta.getEstado() != EstadoSubasta.ADJUDICADA && subasta.getEstado() != EstadoSubasta.FINALIZADA) {
            throw new BusinessRuleException("Solo se pueden iniciar disputas en subastas finalizadas o adjudicadas.");
        }

        subasta.setEstado(EstadoSubasta.EN_DISPUTA);
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
        if (resolucion == EstadoDisputa.RESUELTA_FAVOR_USER) {
            subasta.setEstado(EstadoSubasta.CANCELADA);
        } else {
            subasta.setEstado(EstadoSubasta.ADJUDICADA);
        }
        subastaRepository.save(subasta);

        Usuario perdedor = (resolucion == EstadoDisputa.RESUELTA_FAVOR_USER) ? subasta.getVendedor() : disputa.getIniciador();
        perdedor.setIncidenciasAcumuladas(perdedor.getIncidenciasAcumuladas() + 1);

        if (perdedor.getIncidenciasAcumuladas() >= 3) {
            perdedor.setEstado(EstadoUsuario.BLOQUEADO);
            perdedor.setBloqueadoPor(admin);
            perdedor.setFechaBloqueo(LocalDateTime.now());
            perdedor.setMotivoBloqueo("Acumulación de 3 incidencias.");
        }
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

    public DisputaResponseDTO getById(Long id) {
        Disputa disputa = disputaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Disputa no encontrada con ID: " + id));
        return mapToResponse(disputa);
    }

    public List<DisputaResponseDTO> getDisputesByEstado(EstadoDisputa estado) {
        return disputaRepository.findByEstado(estado).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<DisputaResponseDTO> getDisputesByAdmin(Long adminId) {
        return disputaRepository.findByAdminResolutorId(adminId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private DisputaResponseDTO mapToResponse(Disputa disputa) {
        return DisputaResponseDTO.builder()
                .id(disputa.getId())
                .subastaId(disputa.getSubasta().getId())
                .iniciadorId(disputa.getIniciador().getId())
                .iniciadorUsername(disputa.getIniciador().getUsername())
                .adminResolutorId(disputa.getAdminResolutor() != null ? disputa.getAdminResolutor().getId() : null)
                .adminResolutorUsername(disputa.getAdminResolutor() != null ? disputa.getAdminResolutor().getUsername() : null)
                .motivoApertura(disputa.getMotivoApertura())
                .justificacionResolucion(disputa.getJustificacionResolucion())
                .estado(disputa.getEstado())
                .fechaApertura(disputa.getFechaApertura())
                .fechaResolucion(disputa.getFechaResolucion())
                .build();
    }
}
