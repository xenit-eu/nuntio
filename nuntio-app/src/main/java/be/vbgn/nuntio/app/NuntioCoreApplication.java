package be.vbgn.nuntio.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties
@EnableScheduling
@ComponentScan(basePackages = "be.vbgn.nuntio")
public class NuntioCoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(NuntioCoreApplication.class, args);
	}

}
