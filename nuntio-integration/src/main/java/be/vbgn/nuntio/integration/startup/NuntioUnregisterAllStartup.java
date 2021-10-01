package be.vbgn.nuntio.integration.startup;

import be.vbgn.nuntio.api.registry.ServiceRegistry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.ConfigurableApplicationContext;

@Slf4j
@AllArgsConstructor
public class NuntioUnregisterAllStartup implements NuntioApplicationStartup {

    private ServiceRegistry registry;
    private ConfigurableApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Unregistering all services");
        registry.findServices().forEach(registry::unregisterService);
        log.info("All services unregistered");
        applicationContext.close();
    }

}
