package eu.xenit.nuntio.engine.metrics;

import eu.xenit.nuntio.api.registry.errors.RegistryOperationException;
import eu.xenit.nuntio.api.registry.metrics.RegistryMetrics.ThrowingRunnable;
import eu.xenit.nuntio.api.registry.metrics.RegistryMetrics.ThrowingSupplier;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
final class RegistryOperationMetrics {

    private final Timer operationTimer;
    private final Counter failureCounter;
    private final Counter successCounter;

    static RegistryOperationMetrics create(MeterRegistry meterRegistry, String registryType, String operationName) {
        return new RegistryOperationMetrics(
                meterRegistry.timer("nuntio.registry.operation.duration", "registry", registryType, "operation", operationName),
                meterRegistry.counter("nuntio.registry.operation.failure", "registry", registryType, "operation", operationName),
                meterRegistry.counter("nuntio.registry.operation.success", "registry", registryType, "operation", operationName)
        );
    }

    @SneakyThrows(RegistryOperationException.class)
    public void record(Runnable runnable) {
        record((ThrowingRunnable<? extends RegistryOperationException>) runnable::run);
    }

    public <E extends RegistryOperationException> void record(ThrowingRunnable<E> runnable) throws E {
        record((ThrowingSupplier<?, E>) () -> {
            runnable.run();
            return null;
        });
    }

    @SneakyThrows(RegistryOperationException.class)
    public <T> T record(Supplier<T> runnable) {
        return record((ThrowingSupplier<T, RegistryOperationException>) runnable::get);
    }

    @SneakyThrows
    public <T, E extends RegistryOperationException> T record(ThrowingSupplier<T, E> runnable) throws E {
        boolean hasFailed = true;
        try {
            T result = operationTimer.recordCallable(runnable::get);
            hasFailed = false;
            return result;
        } finally {
            if (hasFailed) {
                failureCounter.increment();
            } else {
                successCounter.increment();
            }
        }
    }

}
