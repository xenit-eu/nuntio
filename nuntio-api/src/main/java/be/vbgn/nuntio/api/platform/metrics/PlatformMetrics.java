package be.vbgn.nuntio.api.platform.metrics;

import be.vbgn.nuntio.api.platform.PlatformServiceEvent;

public interface PlatformMetrics {

    void event(PlatformServiceEvent.EventType eventType);
}
