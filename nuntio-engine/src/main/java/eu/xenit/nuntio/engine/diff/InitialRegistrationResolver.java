package eu.xenit.nuntio.engine.diff;

import eu.xenit.nuntio.api.registry.CheckType;
import eu.xenit.nuntio.api.registry.ServiceRegistry;
import eu.xenit.nuntio.engine.EngineProperties;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class InitialRegistrationResolver implements Consumer<Diff> {
    private ServiceRegistry registry;
    private EngineProperties engineProperties;


    @Override
    public void accept(Diff diff) {
        diff.cast(EqualService.class).ifPresent(equalService -> {
            if(engineProperties.getChecks().isHeartbeat()) {
                registry.registerCheck(equalService.getRegistryServiceIdentifier(), CheckType.HEARTBEAT);
            }
            if(engineProperties.getChecks().isHealthcheck() && equalService.getDescription().getHealth().isPresent()) {
                registry.registerCheck(equalService.getRegistryServiceIdentifier(), CheckType.HEALTHCHECK);
            }
        });

    }
}
