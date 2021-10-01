package be.vbgn.nuntio.app;

import be.vbgn.nuntio.integration.EnableNuntio;
import be.vbgn.nuntio.platform.docker.DockerConfiguration;
import be.vbgn.nuntio.registry.consul.ConsulConfiguration;
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
        SpringApplication.run(NuntioCoreApplication.class, args);
    }

}
