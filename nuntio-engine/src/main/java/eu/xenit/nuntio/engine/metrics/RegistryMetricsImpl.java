package eu.xenit.nuntio.engine.metrics;

import eu.xenit.nuntio.api.registry.RegistryServiceIdentifier;
import eu.xenit.nuntio.api.registry.errors.ServiceDeregistrationException;
import eu.xenit.nuntio.api.registry.errors.ServiceOperationException;
import eu.xenit.nuntio.api.registry.errors.ServiceRegistrationException;
import eu.xenit.nuntio.api.registry.metrics.RegistryMetrics;
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
    public RegistryServiceIdentifier registerService(ThrowingSupplier<? extends RegistryServiceIdentifier, ? extends ServiceRegistrationException> runnable) throws ServiceRegistrationException {
        return registerServiceMetrics.record(runnable);
    }

    @Override
    public void unregisterService(ThrowingRunnable<? extends ServiceDeregistrationException> runnable) throws ServiceDeregistrationException {
        unregisterServiceMetrics.record(runnable);
    }

    @Override
    public void updateCheck(ThrowingRunnable<? extends ServiceOperationException> runnable) throws ServiceOperationException{
        updateCheckMetrics.record(runnable);
    }

    @Override
    public void unregisterCheck(ThrowingRunnable<? extends ServiceOperationException> runnable) throws ServiceOperationException{
        unregisterCheckMetrics.record(runnable);
    }

    @Override
    public void registerCheck(ThrowingRunnable<? extends ServiceOperationException> runnable) throws ServiceOperationException{
        registerCheckMetrics.record(runnable);
    }
}
