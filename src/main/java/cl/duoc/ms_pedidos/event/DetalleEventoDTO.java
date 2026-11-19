
package cl.duoc.ms_pedidos.event;

public record DetalleEventoDTO(
        Long productoId,
        String nombreProducto,
        Double precioUnitario,
        Integer cantidad
) {
}
