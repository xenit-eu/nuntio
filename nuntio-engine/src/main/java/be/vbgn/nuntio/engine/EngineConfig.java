package be.vbgn.nuntio.engine;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("nuntio.engine")
public class EngineConfig {

    LiveWatchConfig live = new LiveWatchConfig();

    @Data
    public static class LiveWatchConfig {

        boolean blocking = true;
        Duration delay = Duration.of(1, ChronoUnit.SECONDS);
    }

}
