package eu.xenit.nuntio.engine.diff;

import eu.xenit.nuntio.api.registry.CheckType;
import eu.xenit.nuntio.api.registry.ServiceRegistry;
import eu.xenit.nuntio.api.registry.errors.ServiceOperationException;
import eu.xenit.nuntio.engine.EngineProperties;
import eu.xenit.nuntio.engine.failure.FailureReporter;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class InitialRegistrationResolver implements Consumer<Diff> {
    private ServiceRegistry registry;
    private FailureReporter failureReporter;
    private EngineProperties engineProperties;


    @Override
    public void accept(Diff diff) {
        diff.cast(EqualService.class).ifPresent(equalService -> {
            try {
                if (engineProperties.getChecks().isHeartbeat()) {
                    registry.registerCheck(equalService.getRegistryServiceIdentifier(), CheckType.HEARTBEAT);
                }
                if (engineProperties.getChecks().isHealthcheck() && equalService.getDescription().getHealth()
                        .isPresent()) {
                    registry.registerCheck(equalService.getRegistryServiceIdentifier(), CheckType.HEALTHCHECK);
                }
            } catch(ServiceOperationException e) {
                log.error("Failed to register service {} checks", equalService.getRegistryServiceIdentifier(), e);
                failureReporter.reportRegistryFailure(e);
            }
        });

    }
}
