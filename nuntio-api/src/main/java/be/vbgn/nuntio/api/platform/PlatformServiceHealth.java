package be.vbgn.nuntio.api.platform;

import lombok.ToString;
import lombok.Value;

@Value
public class PlatformServiceHealth {

    HealthStatus healthStatus;
    @ToString.Exclude
    String log;

    public enum HealthStatus {
        STARTING,
        HEALTHY,
        UNHEALTHY
    }

}
