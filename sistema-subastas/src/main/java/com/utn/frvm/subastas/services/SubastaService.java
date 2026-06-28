package com.utn.frvm.subastas.services;

import com.utn.frvm.subastas.dtos.SubastaRequestDTO;
import com.utn.frvm.subastas.dtos.SubastaResponseDTO;
import com.utn.frvm.subastas.entities.HistorialEstado;
import com.utn.frvm.subastas.entities.Producto;
import com.utn.frvm.subastas.entities.Subasta;
import com.utn.frvm.subastas.entities.Usuario;
import com.utn.frvm.subastas.enums.EstadoSubasta;
import com.utn.frvm.subastas.enums.RolUsuario;
import com.utn.frvm.subastas.enums.TipoNotificacion;
import com.utn.frvm.subastas.exceptions.BusinessRuleException;
import com.utn.frvm.subastas.exceptions.ResourceNotFoundException;
import com.utn.frvm.subastas.repositories.HistorialEstadoRepository;
import com.utn.frvm.subastas.repositories.ProductoRepository;
import com.utn.frvm.subastas.repositories.PujaRepository;
import com.utn.frvm.subastas.repositories.SubastaRepository;
import com.utn.frvm.subastas.repositories.UsuarioRepository;

import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SubastaService {

    private final SubastaRepository subastaRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;
    private final HistorialEstadoRepository historialEstadoRepository;
    private final NotificacionService notificacionService;
    private final PujaRepository pujaRepository;

    public SubastaService(SubastaRepository subastaRepository, ProductoRepository productoRepository,
                          UsuarioRepository usuarioRepository, HistorialEstadoRepository historialEstadoRepository,
                          NotificacionService notificacionService, PujaRepository pujaRepository) {
        this.subastaRepository = subastaRepository;
        this.productoRepository = productoRepository;
        this.usuarioRepository = usuarioRepository;
        this.historialEstadoRepository = historialEstadoRepository;
        this.notificacionService = notificacionService;
        this.pujaRepository = pujaRepository;
    }

    @Transactional
    public SubastaResponseDTO create(SubastaRequestDTO request) {
        if (request.getEstado() != EstadoSubasta.BORRADOR && request.getEstado() != EstadoSubasta.PUBLICADA) {
            throw new BusinessRuleException("Una nueva subasta debe crearse en estado BORRADOR o PUBLICADA.");
        }
        if (request.getPrecioBase() == null ||  request.getPrecioBase().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("El precio base debe ser mayor a cero.");}

        if (request.getIncrementoMinimoPuja() == null || request.getIncrementoMinimoPuja().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException( "El incremento mínimo debe ser mayor a cero.");
        }

        if (request.getFechaInicio() == null || request.getFechaCierre() == null) {
            throw new BusinessRuleException( "Las fechas de inicio y cierre son obligatorias.");
        }

        if (!request.getFechaInicio().isBefore(request.getFechaCierre())) {
            throw new BusinessRuleException( "La fecha de inicio debe ser anterior a la fecha de cierre.");
        }

        Usuario vendedor = usuarioRepository.findById(request.getVendedorId())
                .orElseThrow(() -> new ResourceNotFoundException("Vendedor no encontrado con ID: " + request.getVendedorId()));
        Producto producto = productoRepository.findById(request.getProductoId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con ID: " + request.getProductoId()));

        Subasta subasta = Subasta.builder()
                .vendedor(vendedor)
                .producto(producto)
                .precioBase(request.getPrecioBase())
                .montoActual(request.getPrecioBase())
                .incrementoMinimoPuja(request.getIncrementoMinimoPuja())
                .titulo(request.getTitulo())
                .descripcion(request.getDescripcion())
                .fechaInicio(request.getFechaInicio())
                .fechaCierre(request.getFechaCierre())
                .estado(request.getEstado())
                .build();

        return mapToResponse(subastaRepository.save(subasta));
    }

    @Transactional(readOnly = true)
    public SubastaResponseDTO getById(Long id) {
        Subasta subasta = subastaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subasta no encontrada con ID: " + id));
        return mapToResponse(subasta);
    }

    @Transactional(readOnly = true)
    public List<SubastaResponseDTO> getByVendedorId(Long vendedorId) {
        return subastaRepository.findByVendedorId(vendedorId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SubastaResponseDTO> getByEstado(EstadoSubasta estado) {
        return subastaRepository.findByEstado(estado).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void update(Long id, SubastaRequestDTO request, Long usuarioId) {
        Subasta subasta = subastaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subasta no encontrada con ID: " + id));

        if (request.getPrecioBase() == null || request.getPrecioBase().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("El precio base debe ser mayor a cero.");}

        if (request.getIncrementoMinimoPuja() == null || request.getIncrementoMinimoPuja().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessRuleException("El incremento mínimo debe ser mayor a cero.");}

        if (request.getFechaInicio() == null || request.getFechaCierre() == null) {
            throw new BusinessRuleException( "Las fechas de inicio y cierre son obligatorias.");}

        if (!request.getFechaInicio().isBefore(request.getFechaCierre())) {
            throw new BusinessRuleException("La fecha de inicio debe ser anterior a la fecha de cierre.");}

        // VALIDAR QUE EL USUARIO ES EL VENDEDOR
        if (!subasta.getVendedor().getId().equals(usuarioId)) {
        throw new BusinessRuleException(
            "Solo el vendedor puede actualizar una subasta", 
            HttpStatus.FORBIDDEN
        );
    }

        // VALIDAR QUE NO HAYA PUJAS (no se puede cambiar precio si hay pujas)
        long pujaCount = pujaRepository.countBySubastaId(id);
        if (pujaCount > 0) {
            throw new BusinessRuleException(
                "No se puede actualizar una subasta que ya tiene pujas"
            );
        }
        
        subasta.setPrecioBase(request.getPrecioBase());
        subasta.setIncrementoMinimoPuja(request.getIncrementoMinimoPuja());
        subasta.setTitulo(request.getTitulo());
        subasta.setDescripcion(request.getDescripcion());
        subasta.setFechaInicio(request.getFechaInicio());
        subasta.setFechaCierre(request.getFechaCierre());
        subasta.setEstado(request.getEstado());
        subastaRepository.save(subasta);
    }

    @Transactional
    public void closeAuction(Long id) {
        Subasta subasta = subastaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subasta no encontrada con ID: " + id));

        if (subasta.getGanadorActual() != null) {
            subasta.setEstado(EstadoSubasta.ADJUDICADA);
            subasta.setGanador(subasta.getGanadorActual());
            subasta.setPrecioFinal(subasta.getMontoActual());
            subasta.setFechaAdjudicacion(LocalDateTime.now());
            subastaRepository.save(subasta);

            notificacionService.sendNotification(subasta.getGanador().getId(), subasta.getId(),
                    TipoNotificacion.GANADOR, "¡Felicidades! Has ganado la subasta: " + subasta.getTitulo());
            notificacionService.sendNotification(subasta.getVendedor().getId(), subasta.getId(),
                    TipoNotificacion.VENDEDOR, "Tu subasta '" + subasta.getTitulo() + "' ha sido adjudicada a " + subasta.getGanador().getUsername());
        } else {
            subasta.setEstado(EstadoSubasta.FINALIZADA);
            subastaRepository.save(subasta);

            notificacionService.sendNotification(subasta.getVendedor().getId(), subasta.getId(),
                    TipoNotificacion.VENDEDOR, "Tu subasta '" + subasta.getTitulo() + "' ha finalizado sin ofertas.");
        }
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cerrarSubastasExpiradas() {
        LocalDateTime now = LocalDateTime.now();
        List<Subasta> expiradas = subastaRepository.findByEstadoAndFechaCierreBeforeOrEqual(EstadoSubasta.ACTIVA, now);
        for (Subasta subasta : expiradas) {
            EstadoSubasta anterior = subasta.getEstado();
            subasta.setEstado(EstadoSubasta.FINALIZADA);
            if (subasta.getGanadorActual() != null) {
                subasta.setGanador(subasta.getGanadorActual());
                subasta.setPrecioFinal(subasta.getMontoActual());
                subasta.setFechaAdjudicacion(now);
            }
            subastaRepository.save(subasta);

            HistorialEstado historial = HistorialEstado.builder()
                    .subasta(subasta)
                    .usuarioResponsable(subasta.getVendedor())
                    .estadoAnterior(anterior)
                    .estadoNuevo(EstadoSubasta.FINALIZADA)
                    .motivo("Cierre automático por expiración de tiempo (programado)")
                    .build();
            historialEstadoRepository.save(historial);

            notificacionService.sendNotification(subasta.getVendedor().getId(), subasta.getId(),
                    TipoNotificacion.VENDEDOR, "Tu subasta '" + subasta.getTitulo() + "' ha finalizado por expiración de tiempo.");

            if (subasta.getGanador() != null) {
                notificacionService.sendNotification(subasta.getGanador().getId(), subasta.getId(),
                        TipoNotificacion.GANADOR, "La subasta '" + subasta.getTitulo() + "' en la que participaste ha finalizado.");
            }
        }
    }

    @Scheduled(fixedRate = 30000)  // Cada 30 segundos
    @Transactional
    public void activarSubastasDebidas() {
        LocalDateTime now = LocalDateTime.now();
        List<Subasta> porActivar = subastaRepository.findByEstadoAndFechaInicioBeforeOrEqual(
            EstadoSubasta.PUBLICADA, now
        );
        
        for (Subasta subasta : porActivar) {
            subasta.setEstado(EstadoSubasta.ACTIVA);
            subastaRepository.save(subasta);
            
            HistorialEstado historial = HistorialEstado.builder()
                    .subasta(subasta)
                    .usuarioResponsable(subasta.getVendedor())
                    .estadoAnterior(EstadoSubasta.PUBLICADA)
                    .estadoNuevo(EstadoSubasta.ACTIVA)
                    .motivo("Activación automática por inicio de tiempo")
                    .build();
            historialEstadoRepository.save(historial);
            
            notificacionService.sendNotification(
                subasta.getVendedor().getId(),
                subasta.getId(),
                TipoNotificacion.SISTEMA,
                "Tu subasta '" + subasta.getTitulo() + "' ahora está activa"
            );
        }
    }

    @Transactional
    public void cancelAuction(Long id, String motivo, Long usuarioId, RolUsuario rolUsuario) {
        Subasta subasta = subastaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subasta no encontrada con ID: " + id));
        
        // Validar que no está en estado terminal
        if (subasta.getEstado() == EstadoSubasta.CANCELADA || 
            subasta.getEstado() == EstadoSubasta.FINALIZADA || 
            subasta.getEstado() == EstadoSubasta.ADJUDICADA) {
            throw new BusinessRuleException("No se puede cancelar una subasta que ya está finalizada");
        }
        
        // Contar pujas
        long pujasCount = pujaRepository.countBySubastaId(id);
        
        if (pujasCount > 0) {
            // Solo admin puede cancelar con pujas
            if (rolUsuario != RolUsuario.ROLE_ADMIN) {
                throw new BusinessRuleException(
                    "Solo un administrador puede cancelar una subasta con pujas", 
                    HttpStatus.FORBIDDEN
                );
            }
        } else {
            // Si sin pujas, solo vendedor propietario puede cancelar
            if (!subasta.getVendedor().getId().equals(usuarioId)) {
                throw new BusinessRuleException(
                    "Solo el vendedor propietario puede cancelar una subasta sin pujas", 
                    HttpStatus.FORBIDDEN
                );
            }
        }
        
        // Cambiar estado
        EstadoSubasta estadoAnterior = subasta.getEstado();
        subasta.setEstado(EstadoSubasta.CANCELADA);
        subastaRepository.save(subasta);
        
        // Registrar en historial
        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();
        HistorialEstado historial = HistorialEstado.builder()
                .subasta(subasta)
                .usuarioResponsable(usuario)
                .estadoAnterior(estadoAnterior)
                .estadoNuevo(EstadoSubasta.CANCELADA)
                .motivo(motivo)
                .build();
        historialEstadoRepository.save(historial);
        
        // Notificar
        notificacionService.sendNotification(
            subasta.getVendedor().getId(), 
            id,
            TipoNotificacion.SISTEMA, 
            "Tu subasta '" + subasta.getTitulo() + "' ha sido cancelada"
        );
    }


    private SubastaResponseDTO mapToResponse(Subasta subasta) {
        long count = pujaRepository.countBySubastaId(subasta.getId());
        return SubastaResponseDTO.builder()
                .id(subasta.getId())
                .vendedorId(subasta.getVendedor().getId())
                .vendedorUsername(subasta.getVendedor().getUsername())
                .productoId(subasta.getProducto().getId())
                .productoNombre(subasta.getProducto().getNombre())
                .ganadorId(subasta.getGanador() != null ? subasta.getGanador().getId() : null)
                .ganadorUsername(subasta.getGanador() != null ? subasta.getGanador().getUsername() : null)
                .ganadorActualId(subasta.getGanadorActual() != null ? subasta.getGanadorActual().getId() : null)
                .ganadorActualUsername(subasta.getGanadorActual() != null ? subasta.getGanadorActual().getUsername() : null)
                .precioBase(subasta.getPrecioBase())
                .precioFinal(subasta.getPrecioFinal())
                .fechaCreacion(subasta.getFechaCreacion())
                .fechaInicio(subasta.getFechaInicio())
                .fechaCierre(subasta.getFechaCierre())
                .fechaAdjudicacion(subasta.getFechaAdjudicacion())
                .incrementoMinimoPuja(subasta.getIncrementoMinimoPuja())
                .montoActual(subasta.getMontoActual())
                .titulo(subasta.getTitulo())
                .descripcion(subasta.getDescripcion())
                .estado(subasta.getEstado())
                .cantidadPujas(count)
                .build();
    }
}
