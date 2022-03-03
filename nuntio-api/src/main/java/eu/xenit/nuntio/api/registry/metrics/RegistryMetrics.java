package eu.xenit.nuntio.api.registry.metrics;

import eu.xenit.nuntio.api.registry.RegistryServiceDescription;
import eu.xenit.nuntio.api.registry.RegistryServiceIdentifier;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public interface RegistryMetrics {

    Set<? extends RegistryServiceIdentifier> findServices(
            Supplier<? extends Set<? extends RegistryServiceIdentifier>> runnable);

    Map<? extends RegistryServiceIdentifier, RegistryServiceDescription> findServiceDescriptions(
            Supplier<? extends Map<? extends RegistryServiceIdentifier, RegistryServiceDescription>> runnable);

    RegistryServiceIdentifier registerService(Supplier<? extends RegistryServiceIdentifier> runnable);

    void unregisterService(Runnable runnable);

    void updateCheck(Runnable runnable);

    void unregisterCheck(Runnable runnable);

    void registerCheck(Runnable runnable);
}
