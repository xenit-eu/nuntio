package be.vbgn.nuntio.registry.consul;


import be.vbgn.nuntio.api.registry.CheckType;
import java.util.Map;
import lombok.Data;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties("nuntio.consul")
@NonFinal
public class ConsulConfig {

    String host;
    int port;

    String token = null;

    Map<CheckType, CheckConfig> checks;

    @Value
    @NonFinal
    @ConstructorBinding
    static class CheckConfig {

        String deregisterCriticalServiceAfter;
        String ttl;
    }
}
