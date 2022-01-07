package eu.xenit.nuntio.integration.startup;

import eu.xenit.nuntio.api.registry.ServiceRegistry;
import eu.xenit.nuntio.api.registry.errors.ServiceDeregistrationException;
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
        registry.findServices().forEach(serviceIdentifier -> {
            try {
                registry.unregisterService(serviceIdentifier);
            } catch (ServiceDeregistrationException e) {
                log.error("Failed to unregister service {}", serviceIdentifier, e);
            }
        });
        log.info("All services unregistered");
        applicationContext.close();
    }
}
