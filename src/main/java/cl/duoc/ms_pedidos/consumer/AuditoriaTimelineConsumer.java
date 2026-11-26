package cl.duoc.ms_pedidos.consumer;

import cl.duoc.ms_pedidos.event.EventoAuditadoDTO;
import cl.duoc.ms_pedidos.service.ConfirmacionAuditoriaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/**
 * Escucha audit.timeline: ms-auditoria avisa que registró un evento de un pedido.
 * Cierra el ciclo: ms-pedidos publica en orders.events y recibe la confirmación aquí.
 *
 * Un mensaje mal formado se registra y se descarta (no tiene sentido reintentarlo).
 * Un error de base de datos sí se propaga para que KafkaErrorConfig lo reintente.
 */
@Component
public class AuditoriaTimelineConsumer {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaTimelineConsumer.class);

    private final ConfirmacionAuditoriaService confirmaciones;
    private final ObjectMapper objectMapper;

    public AuditoriaTimelineConsumer(ConfirmacionAuditoriaService confirmaciones, ObjectMapper objectMapper) {
        this.confirmaciones = confirmaciones;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "${app.kafka.topic.timeline:audit.timeline}", groupId = "${spring.kafka.consumer.group-id}")
    public void recibir(String mensaje) {
        EventoAuditadoDTO confirmacion;
        try {
            confirmacion = objectMapper.readValue(mensaje, EventoAuditadoDTO.class);
        } catch (JacksonException | IllegalArgumentException e) {
            log.warn("Confirmación de auditoría ilegible, se descarta: {}", e.getMessage());
            return;
        }

        if (confirmacion == null || !confirmacion.esValido()) {
            log.warn("Confirmación de auditoría incompleta, se descarta: {}", mensaje);
            return;
        }

        confirmaciones.registrar(confirmacion);
    }
}
