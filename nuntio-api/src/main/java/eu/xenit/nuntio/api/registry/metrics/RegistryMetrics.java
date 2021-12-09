package eu.xenit.nuntio.api.registry.metrics;

import eu.xenit.nuntio.api.registry.RegistryServiceIdentifier;
import java.util.Set;
import java.util.function.Supplier;

public interface RegistryMetrics {

    Set<? extends RegistryServiceIdentifier> findServices(
            Supplier<? extends Set<? extends RegistryServiceIdentifier>> runnable);

    RegistryServiceIdentifier registerService(Supplier<? extends RegistryServiceIdentifier> runnable);

    void unregisterService(Runnable runnable);

    void updateCheck(Runnable runnable);

    void unregisterCheck(Runnable runnable);

    void registerCheck(Runnable runnable);
}
