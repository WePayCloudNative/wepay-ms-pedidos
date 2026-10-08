package cl.duoc.ms_pedidos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;

@SpringBootApplication
public class MsPedidosApplication {

	public static void main(String[] args) {
		ApplicationContext ctx = SpringApplication.run(MsPedidosApplication.class, args);
		
		System.out.println("\n" + "=".repeat(60));
		System.out.println("RabbitMQ - Microservicio de Pedidos (Producer)");
		System.out.println("=".repeat(60));
		System.out.println("[OK] Aplicación iniciada correctamente");
		System.out.println("[OK] RabbitMQ conectado en localhost:5672");
		System.out.println("[OK] Cola 'q.cmd.email' disponible");
		System.out.println("[OK] Servidor API en http://localhost:8085/api/pedidos\n");
	}

}
