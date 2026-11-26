package cl.duoc.ms_pedidos.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pedidos")
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Soportado por Oracle 12c+
    private Long id;
    
    private String usuarioOid;
    private Double total;
    private LocalDateTime fechaRegistro;
    
    // Implementación de la máquina de estados estricta
    @Enumerated(EnumType.STRING)
    private EstadoPedido estado;

    // Confirmación de auditoría (llega por audit.timeline): cuándo se registró el último
    // evento del pedido y en qué estado lo dejó. Si coincide con "estado", auditoría está al día.
    private LocalDateTime auditadoEn;
    private String estadoAuditado;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "pedido_id")
    private List<DetallePedido> detalles = new ArrayList<>();

    public Pedido() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public String getUsuarioOid() { return usuarioOid; }
    public void setUsuarioOid(String usuarioOid) { this.usuarioOid = usuarioOid; }
    
    public Double getTotal() { return total; }
    public void setTotal(Double total) { this.total = total; }
    
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime fechaRegistro) { this.fechaRegistro = fechaRegistro; }
    
    public EstadoPedido getEstado() { return estado; }
    public void setEstado(EstadoPedido estado) { this.estado = estado; }

    public LocalDateTime getAuditadoEn() { return auditadoEn; }
    public void setAuditadoEn(LocalDateTime auditadoEn) { this.auditadoEn = auditadoEn; }

    public String getEstadoAuditado() { return estadoAuditado; }
    public void setEstadoAuditado(String estadoAuditado) { this.estadoAuditado = estadoAuditado; }

    public List<DetallePedido> getDetalles() { return detalles; }
    public void setDetalles(List<DetallePedido> detalles) { this.detalles = detalles; }
}