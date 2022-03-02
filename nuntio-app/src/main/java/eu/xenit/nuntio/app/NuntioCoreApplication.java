package eu.xenit.nuntio.app;

import eu.xenit.nuntio.integration.EnableNuntio;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableNuntio
public class NuntioCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(NuntioCoreApplication.class);
    }

}
