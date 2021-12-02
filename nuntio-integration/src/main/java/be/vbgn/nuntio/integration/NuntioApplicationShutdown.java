package be.vbgn.nuntio.integration;

import be.vbgn.nuntio.api.registry.CheckType;
import be.vbgn.nuntio.api.registry.RegistryServiceIdentifier;
import be.vbgn.nuntio.api.registry.ServiceRegistry;
import be.vbgn.nuntio.engine.EngineProperties;
import be.vbgn.nuntio.engine.EngineProperties.ShutdownMode;
import java.util.Arrays;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;

@Slf4j
@AllArgsConstructor
public class NuntioApplicationShutdown implements ApplicationListener<ContextClosedEvent> {

    private final ServiceRegistry registry;
    private final LiveWatchManager liveWatchManager;
    private final EngineProperties engineProperties;

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        liveWatchManager.destroy();
        log.info("Disabling anti-entropy");
        engineProperties.getAntiEntropy().setEnabled(false);

        switch(engineProperties.getShutdownMode()) {
            case UNREGISTER_SERVICES:
                log.info("Shutting down application, unregistering all services.");
                registry.findServices()
                        .forEach(registry::unregisterService);
                break;
            case UNREGISTER_CHECKS:
                log.info("Shutting down application, unregistering all checks.");
                registry.findServices()
                        .forEach(serviceIdentifier -> Arrays.stream(CheckType.values()).forEach(checkType -> registry.unregisterCheck(serviceIdentifier, checkType)));
                break;
            case UNREGISTER_HEARTBEAT:
                log.info("Shutting down application, unregistering heartbeat check.");
                registry.findServices()
                        .forEach(serviceIdentifier -> registry.unregisterCheck(serviceIdentifier, CheckType.HEARTBEAT));
                break;
            case NO_UNREGISTER:
                log.info("Shutting down application, not unregistering anything.");
                break;
        }
    }
}
