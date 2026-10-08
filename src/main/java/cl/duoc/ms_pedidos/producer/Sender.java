package cl.duoc.ms_pedidos.producer;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class Sender {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void enviarComandoCorreo(String destinatario, String mensaje) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
            // Para mantener la simplicidad de la guía, enviamos un String plano[cite: 9]
            String payload = String.format("[%s] TO: %s | MSG: %s", timestamp, destinatario, mensaje);
            
            // Envía a la routingKey que coincide con el nombre de la cola[cite: 10]
            rabbitTemplate.convertAndSend("q.cmd.email", payload);
            System.out.println(" [OK] Mensaje enviado: '" + payload + "'");
            
        } catch (Exception e) {
            System.err.println("[ERROR] Error enviando mensaje: " + e.getMessage());
            e.printStackTrace();
        }
    }
}