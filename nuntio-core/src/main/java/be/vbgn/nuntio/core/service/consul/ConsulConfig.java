package be.vbgn.nuntio.core.service.consul;


import be.vbgn.nuntio.core.service.CheckType;
import java.util.Map;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@Value
@ConstructorBinding
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
