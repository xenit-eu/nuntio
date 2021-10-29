package be.vbgn.nuntio.integration.actuators;

import be.vbgn.nuntio.api.registry.CheckType;
import be.vbgn.nuntio.api.registry.ServiceRegistry;
import be.vbgn.nuntio.engine.EngineProperties;
import be.vbgn.nuntio.integration.NuntioApplicationShutdown;
import be.vbgn.nuntio.integration.startup.NuntioApplicationNormalStartup;
import java.util.Arrays;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

@Endpoint(
    id = "shutdown"
)
@AllArgsConstructor
@Slf4j
public class ShutdownEndpoint implements ApplicationContextAware {
    private ServiceRegistry serviceRegistry;
    private EngineProperties engineProperties;
    private NuntioApplicationNormalStartup applicationStartup;
    private NuntioApplicationShutdown applicationShutdown;
    private final org.springframework.boot.actuate.context.ShutdownEndpoint springShutdownEndpoint =  new org.springframework.boot.actuate.context.ShutdownEndpoint();


    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        springShutdownEndpoint.setApplicationContext(applicationContext);
    }


    private enum ShutdownKind {
        UNREGISTER_SERVICES,
        UNREGISTER_HEARTBEAT,
        UNREGISTER_CHECKS,
        NO_UNREGISTER
    }

    @WriteOperation
    public Map<String, String> shutdown(ShutdownKind kind) {
        applicationStartup.shutdownLiveWatch();
        log.info("Disabling anti-entropy");
        engineProperties.getAntiEntropy().setEnabled(false);

        switch(kind) {
            case UNREGISTER_SERVICES:
                log.info("Shutting down application, unregistering all services.");
                serviceRegistry.findServices()
                        .forEach(serviceRegistry::unregisterService);
                break;
            case UNREGISTER_CHECKS:
                log.info("Shutting down application, unregistering all checks.");
                serviceRegistry.findServices()
                        .forEach(serviceIdentifier -> Arrays.stream(CheckType.values()).forEach(checkType -> serviceRegistry.unregisterCheck(serviceIdentifier, checkType)));
                break;
            case UNREGISTER_HEARTBEAT:
                log.info("Shutting down application, unregistering heartbeat check.");
                serviceRegistry.findServices()
                        .forEach(serviceIdentifier -> serviceRegistry.unregisterCheck(serviceIdentifier, CheckType.HEARTBEAT));
                break;
            case NO_UNREGISTER:
                log.info("Shutting down application, not unregistering anything.");
                break;
        }
        applicationShutdown.disable();

        log.info("Shutting down application");
        return springShutdownEndpoint.shutdown();
    }
}
