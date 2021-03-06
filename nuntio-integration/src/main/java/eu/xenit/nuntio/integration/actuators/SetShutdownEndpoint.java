package eu.xenit.nuntio.integration.actuators;

import eu.xenit.nuntio.engine.EngineProperties;
import eu.xenit.nuntio.engine.EngineProperties.ShutdownMode;
import java.util.Collections;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;

@Endpoint(
    id = "setShutdown"
)
@AllArgsConstructor
@Slf4j
public class SetShutdownEndpoint {
    private EngineProperties engineProperties;

    @WriteOperation
    public Map<String, String> setShutdown(ShutdownMode shutdownMode) {
        log.info("Setting shutdown mode to {}", shutdownMode);
        engineProperties.setShutdownMode(shutdownMode);
        return Collections.singletonMap("shutdownMode", shutdownMode.name());
    }
}
