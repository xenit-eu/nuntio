package be.vbgn.nuntio.platform.docker;

import java.time.Duration;
import lombok.Data;

@Data
public class DockerProperties {

    private boolean enabled = false;

    private DaemonProperties daemon = new DaemonProperties();
    private WatchProperties watch = new WatchProperties();

    private PortBindConfiguration bind = PortBindConfiguration.PUBLISHED;
    private String labelPrefix = "nuntio.vbgn.be";

    public enum PortBindConfiguration {
        PUBLISHED,
        INTERNAL
    }

    @Data
    public static class DaemonProperties {

        private String host;
        private boolean tlsVerify;
        private String certPath;
    }

    @Data
    private class WatchProperties {

        private Duration rate;

    }
}
