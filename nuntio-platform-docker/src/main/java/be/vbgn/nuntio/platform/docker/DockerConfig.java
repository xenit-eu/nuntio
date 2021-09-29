package be.vbgn.nuntio.platform.docker;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("nuntio.docker")
public class DockerConfig {

    private DaemonConfig daemon;


    @Data
    public static class DaemonConfig {

        private String host;
        private boolean tlsVerify;
        private String certPath;
    }
}
