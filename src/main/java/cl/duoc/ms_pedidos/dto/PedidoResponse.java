package cl.duoc.ms_pedidos.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PedidoResponse(
    Long id,
    String usuarioOid,
    Double total,
    LocalDateTime fechaRegistro,
    String estado,
    List<DetalleResponse> detalles,
    // null hasta que auditoría confirme el primer evento del pedido
    LocalDateTime auditadoEn,
    String estadoAuditado
) {}