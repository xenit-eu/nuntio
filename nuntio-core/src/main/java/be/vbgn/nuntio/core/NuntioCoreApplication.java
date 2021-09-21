package be.vbgn.nuntio.core;

import be.vbgn.nuntio.core.docker.DockerConfig;
import be.vbgn.nuntio.core.service.consul.ConsulConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties({DockerConfig.class, ConsulConfig.class})
@EnableScheduling
public class NuntioCoreApplication {

	public static void main(String[] args) {
		SpringApplication.run(NuntioCoreApplication.class, args);
	}

}
