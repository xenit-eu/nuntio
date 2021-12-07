package be.vbgn.nuntio.engine.metrics;

import be.vbgn.nuntio.api.registry.RegistryServiceIdentifier;
import be.vbgn.nuntio.api.registry.metrics.RegistryMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Set;
import java.util.function.Supplier;

public class RegistryMetricsImpl implements RegistryMetrics {
    private final RegistryOperationMetrics findServicesMetrics;
    private final RegistryOperationMetrics registerServiceMetrics;
    private final RegistryOperationMetrics unregisterServiceMetrics;
    private final RegistryOperationMetrics registerCheckMetrics;
    private final RegistryOperationMetrics unregisterCheckMetrics;
    private final RegistryOperationMetrics updateCheckMetrics;

    public RegistryMetricsImpl(MeterRegistry meterRegistry, String registryType)  {
        findServicesMetrics = RegistryOperationMetrics.create(meterRegistry, registryType, "service.find");
        registerServiceMetrics = RegistryOperationMetrics.create(meterRegistry, registryType, "service.register");
        unregisterServiceMetrics = RegistryOperationMetrics.create(meterRegistry, registryType, "service.unregister");
        registerCheckMetrics = RegistryOperationMetrics.create(meterRegistry, registryType, "check.register");
        unregisterCheckMetrics = RegistryOperationMetrics.create(meterRegistry, registryType, "check.unregister");
        updateCheckMetrics = RegistryOperationMetrics.create(meterRegistry, registryType, "check.update");
    }

    @Override
    public Set<? extends RegistryServiceIdentifier> findServices(
            Supplier<? extends Set<? extends RegistryServiceIdentifier>> runnable) {
        return findServicesMetrics.record(runnable);
    }

    @Override
    public RegistryServiceIdentifier registerService(Supplier<? extends RegistryServiceIdentifier> runnable) {
        return registerServiceMetrics.record(runnable);
    }

    @Override
    public void unregisterService(Runnable runnable) {
        unregisterServiceMetrics.record(runnable);
    }

    @Override
    public void updateCheck(Runnable runnable) {
        updateCheckMetrics.record(runnable);
    }

    @Override
    public void unregisterCheck(Runnable runnable) {
        unregisterCheckMetrics.record(runnable);
    }

    @Override
    public void registerCheck(Runnable runnable) {
        registerCheckMetrics.record(runnable);
    }
}
