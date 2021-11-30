package be.vbgn.nuntio.platform.docker;

import java.time.Duration;
import lombok.Data;

@Data
public class DockerProperties {

    private boolean enabled = false;

    private DaemonProperties daemon = new DaemonProperties();

    private PortBindConfiguration bind = PortBindConfiguration.PUBLISHED;
    private NuntioLabelProperties nuntioLabel =new NuntioLabelProperties();
    private RegistratorCompatibleProperties registratorCompat = new RegistratorCompatibleProperties();

    public enum PortBindConfiguration {
        PUBLISHED,
        INTERNAL
    }

    @Data
    public static class DaemonProperties {

        private String host;
        private boolean tlsVerify = false;
        private String certPath;
    }

    @Data
    public static class NuntioLabelProperties {
        private boolean enabled = true;
        private String prefix = "nuntio.vbgn.be";
    }

    @Data
    public static class RegistratorCompatibleProperties {
        private boolean enabled = false;
        private boolean explicit = false;
    }
}
