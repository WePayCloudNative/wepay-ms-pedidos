
package cl.duoc.ms_pedidos.producer;

import cl.duoc.ms_pedidos.entity.EstadoPedido;
import cl.duoc.ms_pedidos.entity.Pedido;
import cl.duoc.ms_pedidos.event.Actor;
import cl.duoc.ms_pedidos.event.DetalleEventoDTO;
import cl.duoc.ms_pedidos.event.PedidoEventoDTO;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class PedidoEventoProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    private static final String TOPIC = "orders.events";

    public PedidoEventoProducer(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper) {

        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void publicarEvento(
            Pedido pedido,
            String tipoEvento,
            Actor actor,
            EstadoPedido estadoAnterior) {

        List<DetalleEventoDTO> detalles =
                pedido.getDetalles().stream()
                        .map(detalle -> new DetalleEventoDTO(
                                detalle.getProductoId(),
                                detalle.getNombreProducto(),
                                detalle.getPrecioUnitario(),
                                detalle.getCantidad()
                        ))
                        .toList();

        PedidoEventoDTO evento = new PedidoEventoDTO(
                UUID.randomUUID().toString(),
                tipoEvento,
                pedido.getId(),
                pedido.getUsuarioOid(),
                pedido.getTotal(),
                pedido.getEstado().name(),
                pedido.getFechaRegistro(),
                LocalDateTime.now(),
                detalles,
                actor.oid(),
                actor.rol(),
                estadoAnterior == null ? null : estadoAnterior.name()
        );

        String mensaje = objectMapper.writeValueAsString(evento);

        kafkaTemplate.send(
                TOPIC,
                pedido.getId().toString(),
                mensaje
        );

        System.out.println(
                "Evento enviado a Kafka: " + tipoEvento
        );
    }
}
