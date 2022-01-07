package eu.xenit.nuntio.api.registry.metrics;

import eu.xenit.nuntio.api.registry.RegistryServiceIdentifier;
import eu.xenit.nuntio.api.registry.errors.ServiceDeregistrationException;
import eu.xenit.nuntio.api.registry.errors.ServiceOperationException;
import eu.xenit.nuntio.api.registry.errors.ServiceRegistrationException;
import java.util.Set;
import java.util.function.Supplier;

public interface RegistryMetrics {

    @FunctionalInterface
    interface ThrowingSupplier<R, E extends Throwable>  {
        R get() throws E;
    }

    interface ThrowingRunnable<E extends Throwable> {
        void run() throws E;
    }

    Set<? extends RegistryServiceIdentifier> findServices(
            Supplier<? extends Set<? extends RegistryServiceIdentifier>> runnable);

    RegistryServiceIdentifier registerService(ThrowingSupplier<? extends RegistryServiceIdentifier, ? extends ServiceRegistrationException> runnable) throws ServiceRegistrationException;

    void unregisterService(ThrowingRunnable<? extends ServiceDeregistrationException> runnable) throws ServiceDeregistrationException;

    void updateCheck(ThrowingRunnable<? extends ServiceOperationException> runnable) throws ServiceOperationException;

    void unregisterCheck(ThrowingRunnable<? extends ServiceOperationException> runnable) throws ServiceOperationException;

    void registerCheck(ThrowingRunnable<? extends ServiceOperationException> runnable) throws ServiceOperationException;
}
