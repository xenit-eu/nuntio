package be.vbgn.nuntio.engine;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.stereotype.Component;

@Data
@Component
@ConstructorBinding
@ConfigurationProperties("nuntio.engine")
public class EngineConfig {

    LiveWatchConfig live = new LiveWatchConfig();

    @Data
    public static class LiveWatchConfig {

        boolean blocking = true;
    }

}
