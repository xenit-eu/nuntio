package be.vbgn.nuntio.integration;

import be.vbgn.nuntio.api.registry.CheckType;
import be.vbgn.nuntio.api.registry.RegistryServiceIdentifier;
import be.vbgn.nuntio.api.registry.ServiceRegistry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

@Slf4j
@AllArgsConstructor
public class NuntioApplicationShutdown implements ApplicationListener<ContextClosedEvent> {

    private ServiceRegistry registry;

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        log.info("Unregistering health checks on application shutdown");
        for (RegistryServiceIdentifier service : registry.findServices()) {
            registry.unregisterCheck(service, CheckType.HEALTHCHECK);
        }

    }
}
