package communication;

import com.fasterxml.jackson.databind.deser.std.NumberDeserializers;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CommunicationApplication {

	public static int port;

	public static void main(String[] args) {

		port = Integer.parseInt(args[0]);

		SpringApplication.run(CommunicationApplication.class, args);

	}

}
