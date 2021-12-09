package eu.xenit.nuntio.api.platform.metrics;

import eu.xenit.nuntio.api.platform.PlatformServiceEvent;

public interface PlatformMetrics {

    void event(PlatformServiceEvent.EventType eventType);
}
