package be.vbgn.nuntio.api.platform.metrics;

import be.vbgn.nuntio.api.platform.PlatformServiceEvent;
import be.vbgn.nuntio.api.platform.PlatformServiceEvent.EventType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class PlatformMetrics {
    private final Map<EventType, Counter> eventCounters = new HashMap<>();

    public PlatformMetrics(MeterRegistry meterRegistry, String platformType)  {
        Arrays.stream(EventType.values())
                .forEach(eventType -> {
                    eventCounters.put(eventType, meterRegistry.counter("nuntio.platform.event", "platform", platformType, "event", eventType.name()));
                });
    }

    public void event(PlatformServiceEvent.EventType eventType) {
        eventCounters.get(eventType).increment();
    }
}
