package cl.duoc.ms_pedidos.dto;

// Un producto dentro de un pedido, para mostrar el detalle en el front
public record DetalleResponse(
    Long productoId,
    String nombreProducto,
    Double precioUnitario,
    Integer cantidad
) {}
