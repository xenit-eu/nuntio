package be.vbgn.nuntio.api.registry.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.function.Supplier;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
final class OperationMetrics {

    private final Timer operationTimer;
    private final Counter failureCounter;
    private final Counter successCounter;

    static OperationMetrics create(MeterRegistry meterRegistry, String registryType, String operationName) {
        return new OperationMetrics(
                meterRegistry.timer("nuntio.registry.operation.duration", "registry", registryType, "operation", operationName),
                meterRegistry.counter("nuntio.registry.operation.failure", "registry", registryType, "operation", operationName),
                meterRegistry.counter("nuntio.registry.operation.success", "registry", registryType, "operation", operationName)
        );
    }

    public void record(Runnable runnable) {
        record(() -> {
            runnable.run();
            return null;
        });
    }

    public <T> T record(Supplier<T> runnable) {
        boolean hasFailed = true;
        try {
            T result = operationTimer.record(runnable);
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
