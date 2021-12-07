package be.vbgn.nuntio.engine.metrics;

import io.micrometer.core.instrument.MeterRegistry;

public class LiveWatchMetrics extends DiffOperationMetrics {
    private final MeterRegistry meterRegistry;

    public LiveWatchMetrics(MeterRegistry meterRegistry) {
        super(meterRegistry, "live");
        this.meterRegistry = meterRegistry;
    }

    public void blockingTime(Runnable runnable)  {
        meterRegistry.more().longTaskTimer("nuntio.live.blocking").record(runnable);
    }

    public void pollingTime(Runnable runnable) {
        meterRegistry.timer("nuntio.live.polling").record(runnable);
    }
}
