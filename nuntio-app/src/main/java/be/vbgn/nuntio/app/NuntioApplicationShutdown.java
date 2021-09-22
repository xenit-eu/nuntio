package be.vbgn.nuntio.app;

import be.vbgn.nuntio.api.registry.CheckType;
import be.vbgn.nuntio.api.registry.RegistryServiceIdentifier;
import be.vbgn.nuntio.api.registry.ServiceRegistry;
import javax.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@AllArgsConstructor(onConstructor_ = @Autowired)
public class NuntioApplicationShutdown {

    private ServiceRegistry registry;

    @PreDestroy
    public void unregisterChecks() {
        log.info("Unregistering health checks on application shutdown");
        for (RegistryServiceIdentifier service : registry.findServices()) {
            registry.unregisterCheck(service, CheckType.HEALTHCHECK);
        }
    }
}
