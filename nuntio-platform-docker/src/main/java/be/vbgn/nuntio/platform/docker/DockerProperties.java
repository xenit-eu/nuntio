package be.vbgn.nuntio.platform.docker;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("nuntio.docker")
public class DockerProperties {

    private DaemonProperties daemon;


    @Data
    public static class DaemonProperties {

        private String host;
        private boolean tlsVerify;
        private String certPath;
    }
}
