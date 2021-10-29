package be.vbgn.nuntio.engine.diff;

import be.vbgn.nuntio.api.registry.CheckStatus;
import be.vbgn.nuntio.api.registry.CheckType;
import be.vbgn.nuntio.api.registry.ServiceRegistry;
import be.vbgn.nuntio.engine.EngineProperties;
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
