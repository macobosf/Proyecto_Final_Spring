package ec.edu.ups.icc.academiceventsapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class AcademicEventsApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(AcademicEventsApiApplication.class, args);
	}

}
