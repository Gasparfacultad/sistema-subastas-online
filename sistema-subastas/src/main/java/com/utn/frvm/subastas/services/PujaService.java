package com.utn.frvm.subastas.services;

import com.utn.frvm.subastas.dtos.PujaRequestDTO;
import com.utn.frvm.subastas.dtos.PujaResponseDTO;
import com.utn.frvm.subastas.entities.Puja;
import com.utn.frvm.subastas.entities.Subasta;
import com.utn.frvm.subastas.entities.Usuario;
import com.utn.frvm.subastas.enums.EstadoSubasta;
import com.utn.frvm.subastas.enums.EstadoUsuario;
import com.utn.frvm.subastas.enums.TipoNotificacion;
import com.utn.frvm.subastas.exceptions.BusinessRuleException;
import com.utn.frvm.subastas.exceptions.ResourceNotFoundException;
import com.utn.frvm.subastas.repositories.PujaRepository;
import com.utn.frvm.subastas.repositories.SubastaRepository;
import com.utn.frvm.subastas.repositories.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PujaService {

    private final NotificacionService notificacionService;
    private final PujaRepository pujaRepository;
    private final SubastaRepository subastaRepository;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioService usuarioService;

    public PujaService(PujaRepository pujaRepository, SubastaRepository subastaRepository, UsuarioRepository usuarioRepository, NotificacionService notificacionService, UsuarioService usuarioService) {
        this.pujaRepository = pujaRepository;
        this.subastaRepository = subastaRepository;
        this.usuarioRepository = usuarioRepository;
        this.notificacionService = notificacionService;
        this.usuarioService = usuarioService;
    }

    @Transactional
    public PujaResponseDTO realizarPuja(PujaRequestDTO dto, Long compradorId) {
        Subasta subasta = subastaRepository.findByIdForUpdate(dto.getSubastaId())
                .orElseThrow(
                        () -> new ResourceNotFoundException("Subasta no encontrada con ID: " + dto.getSubastaId()));

        Usuario comprador = usuarioRepository.findById(compradorId)
                .orElseThrow(() -> new ResourceNotFoundException("Comprador no encontrado con ID: " + compradorId));
        
        if (comprador.getEstado() != EstadoUsuario.ACTIVO) {
            throw new BusinessRuleException(
            "El usuario se encuentra bloqueado o inactivo.");
        }
        
        if (subasta.getFechaCierre().isBefore(LocalDateTime.now())) {
        throw new BusinessRuleException(
            "La subasta ya se encuentra cerrada.");
        }

        if (subasta.getVendedor().getId().equals(comprador.getId())) {
            throw new BusinessRuleException("El vendedor no puede ofertar en su propia subasta.");
        }

        if (subasta.getEstado() != EstadoSubasta.ACTIVA) {
            throw new BusinessRuleException("La subasta no está activa.");
        }

        long countPujas = pujaRepository.countBySubastaId(subasta.getId());
        if (countPujas == 0) {
            // Primera puja: mayor o igual al precio base
            if (dto.getMonto().compareTo(subasta.getPrecioBase()) < 0) {
                throw new BusinessRuleException("La primera puja debe ser mayor o igual al precio base: $" + subasta.getPrecioBase());
            }
        } else {
            // Siguientes pujas: mayor o igual a montoActual + incrementoMinimo
            BigDecimal threshold = subasta.getMontoActual().add(subasta.getIncrementoMinimoPuja());
            if (dto.getMonto().compareTo(threshold) < 0) {
                throw new BusinessRuleException("El monto de la puja debe ser mayor o igual a " + threshold);
            }
        }
        // GUARDAR EL GANADOR ANTERIOR (si existe)
        Usuario ganadorAnterior = subasta.getGanadorActual();
        
        Puja nuevaPuja = Puja.builder()
                .subasta(subasta)
                .comprador(comprador)
                .monto(dto.getMonto())
                .esGanadora(true)
                .build();

        Puja savedPuja = pujaRepository.save(nuevaPuja);
        pujaRepository.updatePreviousWinningBidsToFalse(subasta.getId(), savedPuja.getId());

        subasta.setMontoActual(dto.getMonto());
        subasta.setGanadorActual(comprador);
        subastaRepository.save(subasta);

        // NUEVO: Notificar al ganador anterior que fue superado
        if (ganadorAnterior != null && !ganadorAnterior.getId().equals(comprador.getId())) {
            notificacionService.sendNotification(
                ganadorAnterior.getId(), 
                subasta.getId(),
                TipoNotificacion.SISTEMA, 
                "¡Has sido superado en la subasta '" + subasta.getTitulo() + 
                "'! Nueva oferta más alta: $" + dto.getMonto()
            );
        }

        return mapToResponse(savedPuja);
    }

    @Transactional
    public PujaResponseDTO placeBid(PujaRequestDTO request) {
        return realizarPuja(request, request.getCompradorId());
    }

    public List<PujaResponseDTO> getBidsBySubastaId(Long subastaId) {
        if (!subastaRepository.existsById(subastaId)) {
            throw new ResourceNotFoundException("Subasta no encontrada con ID: " + subastaId);
        }
        return pujaRepository.findBySubastaIdOrderByMontoDesc(subastaId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private PujaResponseDTO mapToResponse(Puja puja) {
        return PujaResponseDTO.builder()
                .id(puja.getId())
                .subastaId(puja.getSubasta().getId())
                .compradorId(puja.getComprador().getId())
                .compradorUsername(puja.getComprador().getUsername())
                .monto(puja.getMonto())
                .fechaHora(puja.getFechaHora())
                .esGanadora(puja.getEsGanadora())
                .build();
    }

    @Transactional(readOnly = true)
    public List<PujaResponseDTO> getBidsWithPrivacy(Long subastaId, Long usuarioId) {
        Subasta subasta = subastaRepository.findById(subastaId)
                .orElseThrow(() -> new ResourceNotFoundException("Subasta no encontrada"));
        
        List<Puja> pujas = pujaRepository.findBySubastaIdOrderByMontoDesc(subastaId);
        
        if (subasta.getEstado() == EstadoSubasta.ACTIVA) {
            // Subasta activa: mostrar según permisos
            List<PujaResponseDTO> respuestas = new ArrayList<>();
            
            for (Puja puja : pujas) {
                PujaResponseDTO dto = mapToResponse(puja);
                
                // El usuario ve su propia puja con identidad completa
                if (puja.getComprador().getId().equals(usuarioId)) {
                    // Mantener datos completos
                } 
                // El vendedor ve todas las pujas
                else if (subasta.getVendedor().getId().equals(usuarioId)) {
                    // Mantener datos completos
                }
                // Admin ve todo
                else if (usuarioService.isAdmin(usuarioId)) {
                    // Mantener datos completos
                }
                // Otros usuarios solo ven monto (sin identidad del oferente)
                else {
                    dto.setCompradorUsername("Anónimo #" + puja.getId());
                    dto.setCompradorId(null);
                }
                
                respuestas.add(dto);
            }
            return respuestas;
        } else {
            // Subasta finalizada o adjudicada
            if (subasta.getVendedor().getId().equals(usuarioId)) {
                // Vendedor ve todo
                return pujas.stream().map(this::mapToResponse).collect(Collectors.toList());
            } else if (usuarioService.isAdmin(usuarioId)) {
                // Admin ve todo
                return pujas.stream().map(this::mapToResponse).collect(Collectors.toList());
            } else if (subasta.getGanador() != null && subasta.getGanador().getId().equals(usuarioId)) {
                // Ganador ve su puja
                return pujas.stream()
                    .filter(p -> p.getComprador().getId().equals(usuarioId))
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
            } else {
                // Otros: ven solo el resultado final (monto más alto)
                if (!pujas.isEmpty()) {
                    PujaResponseDTO dto = mapToResponse(pujas.get(0));
                    dto.setCompradorUsername("Anónimo");
                    dto.setCompradorId(null);
                    return List.of(dto);
                }
                return List.of();
            }
        }
    }
}
