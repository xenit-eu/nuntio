package be.vbgn.nuntio.app.startup;

import be.vbgn.nuntio.api.registry.ServiceRegistry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty("nuntio.app.unregister-all")
@AllArgsConstructor(onConstructor_ = @Autowired)
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
