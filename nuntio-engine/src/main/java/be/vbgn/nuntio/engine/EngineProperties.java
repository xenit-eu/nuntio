package be.vbgn.nuntio.engine;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import lombok.Data;

@Data
public class EngineProperties {

    LiveWatchProperties live = new LiveWatchProperties();
    AntiEntropyProperties antiEntropy = new AntiEntropyProperties();
    CheckProperties checks = new CheckProperties();

    @Data
    public static class LiveWatchProperties {

        boolean enabled = true;
        boolean blocking = true;
        Duration delay = Duration.of(1, ChronoUnit.SECONDS);
    }

    @Data
    public static class AntiEntropyProperties {

        boolean enabled = true;
        Duration delay = Duration.of(1, ChronoUnit.MINUTES);
    }

    @Data
    public static class CheckProperties {

        boolean heartbeat = true;
        boolean healthcheck = true;
    }
}