package eu.xenit.nuntio.registry.consul;


import eu.xenit.nuntio.api.registry.CheckType;
import java.util.Map;
import lombok.Data;
import lombok.Value;
import lombok.experimental.NonFinal;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.stereotype.Component;

@Data
@Component
public class ConsulProperties {

    boolean enabled = false;

    String host = "localhost";
    int port = 8500;

    String token = null;

    Map<CheckType, CheckProperties> checks = Map.of(
            CheckType.HEARTBEAT, new CheckProperties("72h", "24h"),
            CheckType.PAUSE, new CheckProperties(null, "5m"),
            CheckType.HEALTHCHECK, new CheckProperties(null, "5m")
    );

    @Value
    @NonFinal
    @ConstructorBinding
    static class CheckProperties {

        String deregisterCriticalServiceAfter;
        String ttl;
    }
}
