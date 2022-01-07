package eu.xenit.nuntio.integration;

import eu.xenit.nuntio.api.registry.CheckType;
import eu.xenit.nuntio.api.registry.ServiceRegistry;
import eu.xenit.nuntio.api.registry.errors.ServiceDeregistrationException;
import eu.xenit.nuntio.api.registry.errors.ServiceOperationException;
import eu.xenit.nuntio.engine.EngineProperties;
import java.util.Arrays;
import lombok.AllArgsConstructor;
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
                        .forEach(serviceIdentifier -> {
                            try {
                                registry.unregisterService(serviceIdentifier);
                            } catch (ServiceDeregistrationException e) {
                                log.error("Failed to unregister service {} during shutdown", serviceIdentifier, e);
                            }
                        });
                break;
            case UNREGISTER_CHECKS:
                log.info("Shutting down application, unregistering all checks.");
                registry.findServices()
                        .forEach(serviceIdentifier -> Arrays.stream(CheckType.values()).forEach(checkType -> {
                            try {
                                registry.unregisterCheck(serviceIdentifier, checkType);
                            } catch (ServiceOperationException e) {
                                log.error("Failed to unregister service {} check {} during shutdown", serviceIdentifier, checkType, e);
                            }
                        }));
                break;
            case UNREGISTER_HEARTBEAT:
                log.info("Shutting down application, unregistering heartbeat check.");
                registry.findServices()
                        .forEach(serviceIdentifier -> {
                            try {
                                registry.unregisterCheck(serviceIdentifier, CheckType.HEARTBEAT);
                            } catch (ServiceOperationException e) {
                                log.error("Failed to unregister service {} heartbeat during shutdown", serviceIdentifier, e);
                            }
                        });
                break;
            case NOTHING:
                log.info("Shutting down application, not unregistering anything.");
                break;
        }
    }
}
