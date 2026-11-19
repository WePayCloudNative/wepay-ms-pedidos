
package cl.duoc.ms_pedidos.event;

import java.time.LocalDateTime;
import java.util.List;

public record PedidoEventoDTO(
        String eventId,
        String tipoEvento,
        Long pedidoId,
        String usuarioOid,
        Double total,
        String estado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaEvento,
        List<DetalleEventoDTO> detalles
) {
}
