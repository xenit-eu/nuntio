package eu.xenit.nuntio.app;

import eu.xenit.nuntio.integration.EnableNuntio;
import eu.xenit.nuntio.platform.docker.DockerConfiguration;
import eu.xenit.nuntio.registry.consul.ConsulConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@EnableNuntio
@Import({
        DockerConfiguration.class,
        ConsulConfiguration.class
})
public class NuntioCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(NuntioCoreApplication.class);
    }

}
