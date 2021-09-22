package be.vbgn.nuntio.api.platform;

import lombok.Value;

@Value
public class PlatformServiceEvent {

    EventType eventType;
    PlatformServiceIdentifier identifier;

    public enum EventType {
        START,
        STOP,
        PAUSE,
        UNPAUSE,
        HEALTHCHECK
    }

}
