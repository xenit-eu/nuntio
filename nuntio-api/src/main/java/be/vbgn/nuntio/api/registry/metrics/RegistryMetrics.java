package be.vbgn.nuntio.api.registry.metrics;

import be.vbgn.nuntio.api.registry.RegistryServiceIdentifier;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Set;
import java.util.function.Supplier;

public class RegistryMetrics {
    private final OperationMetrics findServicesMetrics;
    private final OperationMetrics registerServiceMetrics;
    private final OperationMetrics unregisterServiceMetrics;
    private final OperationMetrics registerCheckMetrics;
    private final OperationMetrics unregisterCheckMetrics;
    private final OperationMetrics updateCheckMetrics;

    public RegistryMetrics(MeterRegistry meterRegistry, String registryType)  {
        findServicesMetrics = OperationMetrics.create(meterRegistry, registryType, "service.find");
        registerServiceMetrics = OperationMetrics.create(meterRegistry, registryType, "service.register");
        unregisterServiceMetrics = OperationMetrics.create(meterRegistry, registryType, "service.unregister");
        registerCheckMetrics = OperationMetrics.create(meterRegistry, registryType, "check.register");
        unregisterCheckMetrics = OperationMetrics.create(meterRegistry, registryType, "check.unregister");
        updateCheckMetrics = OperationMetrics.create(meterRegistry, registryType, "check.update");
    }

    public Set<? extends RegistryServiceIdentifier> findServices(
            Supplier<? extends Set<? extends RegistryServiceIdentifier>> runnable) {
        return findServicesMetrics.record(runnable);
    }

    public RegistryServiceIdentifier registerService(Supplier<? extends RegistryServiceIdentifier> runnable) {
        return registerServiceMetrics.record(runnable);
    }

    public void unregisterService(Runnable runnable) {
        unregisterServiceMetrics.record(runnable);
    }

    public void updateCheck(Runnable runnable) {
        updateCheckMetrics.record(runnable);
    }

    public void unregisterCheck(Runnable runnable) {
        unregisterCheckMetrics.record(runnable);
    }

    public void registerCheck(Runnable runnable) {
        registerCheckMetrics.record(runnable);
    }
}
