package be.vbgn.nuntio.integration;

import be.vbgn.nuntio.api.registry.CheckType;
import be.vbgn.nuntio.api.registry.RegistryServiceIdentifier;
import be.vbgn.nuntio.api.registry.ServiceRegistry;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

@Slf4j
@RequiredArgsConstructor
public class NuntioApplicationShutdown implements ApplicationListener<ContextClosedEvent> {

    private final ServiceRegistry registry;
    private boolean isEnabled = true;

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        if(!isEnabled) {
            return;
        }
        log.info("Unregistering health checks on application shutdown");
        for (RegistryServiceIdentifier service : registry.findServices()) {
            registry.unregisterCheck(service, CheckType.HEALTHCHECK);
        }
    }

    public void disable() {
        isEnabled = false;
    }
}
