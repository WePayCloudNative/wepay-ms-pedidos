package cl.duoc.ms_pedidos.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;

/**
 * Confirmación que ms-auditoria publica en audit.timeline cuando registra un evento.
 * Se ignoran los campos que ms-pedidos no usa.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EventoAuditadoDTO(
        String eventId,
        String tipoEvento,
        Long pedidoId,
        String estadoNuevo,
        LocalDateTime fechaEvento,
        LocalDateTime fechaAuditoria) {

    /** Sin pedido ni fecha no hay nada que marcar. */
    public boolean esValido() {
        return pedidoId != null && fechaAuditoria != null;
    }
}
