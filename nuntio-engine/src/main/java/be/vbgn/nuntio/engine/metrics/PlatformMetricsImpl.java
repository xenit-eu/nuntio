package be.vbgn.nuntio.engine.metrics;

import be.vbgn.nuntio.api.platform.PlatformServiceEvent;
import be.vbgn.nuntio.api.platform.PlatformServiceEvent.EventType;
import be.vbgn.nuntio.api.platform.metrics.PlatformMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class PlatformMetricsImpl implements PlatformMetrics {
    private final Map<EventType, Counter> eventCounters = new HashMap<>();

    public PlatformMetricsImpl(MeterRegistry meterRegistry, String platformType)  {
        Arrays.stream(EventType.values())
                .forEach(eventType -> {
                    eventCounters.put(eventType, meterRegistry.counter("nuntio.platform.event", "platform", platformType, "event", eventType.name()));
                });
    }

    @Override
    public void event(PlatformServiceEvent.EventType eventType) {
        eventCounters.get(eventType).increment();
    }
}
