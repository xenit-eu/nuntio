package eu.xenit.nuntio.engine.metrics;

import eu.xenit.nuntio.engine.diff.AddService;
import eu.xenit.nuntio.engine.diff.Diff;
import eu.xenit.nuntio.engine.diff.EqualService;
import eu.xenit.nuntio.engine.diff.RemoveService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.function.Consumer;

public class DiffOperationMetrics implements Consumer<Diff> {
    private final Counter operationAdd;
    private final Counter operationUpdate;
    private final Counter operationRemove;
    private final Counter failures;

    public DiffOperationMetrics(MeterRegistry meterRegistry, String systemType)  {
        operationAdd = meterRegistry.counter("nuntio.engine.operation", "engine", systemType, "operation", "add");
        operationUpdate = meterRegistry.counter("nuntio.engine.operation", "engine", systemType, "operation", "update");
        operationRemove = meterRegistry.counter("nuntio.engine.operation", "engine", systemType, "operation", "remove");
        failures = meterRegistry.counter("nuntio.engine.failure", "engine", systemType);
    }

    public void add() {
        operationAdd.increment();
    }

    public void remove() {
        operationRemove.increment();
    }

    public void update() {
        operationUpdate.increment();
    }

    public void failure() {
        failures.increment();
    }

    @Override
    public void accept(Diff diff) {
        diff.cast(AddService.class).ifPresent(_unused -> add());
        diff.cast(EqualService.class).ifPresent(_unused -> update());
        diff.cast(RemoveService.class).ifPresent(_unused -> remove());
    }
}
