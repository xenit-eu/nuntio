package eu.xenit.nuntio.engine;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import lombok.Data;

@Data
public class EngineProperties {

    LiveWatchProperties live = new LiveWatchProperties();
    AntiEntropyProperties antiEntropy = new AntiEntropyProperties();
    CheckProperties checks = new CheckProperties();

    ShutdownMode shutdownMode = ShutdownMode.UNREGISTER_CHECKS;

    AddressFamilies serviceAddress = new AddressFamilies();

    Set<String> forcedTags = new HashSet<>();

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

    public enum ShutdownMode {
        UNREGISTER_SERVICES,
        UNREGISTER_HEARTBEAT,
        UNREGISTER_CHECKS,
        NOTHING
    }

    @Data
    public static class AddressFamilies {
        private boolean ipv4 = true;
        private boolean ipv6 = true;
    }
}
