package be.vbgn.nuntio.engine;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("nuntio.engine")
public class EngineProperties {

    LiveWatchProperties live = new LiveWatchProperties();

    @Data
    public static class LiveWatchProperties {

        boolean blocking = true;
        Duration delay = Duration.of(1, ChronoUnit.SECONDS);
    }

}
