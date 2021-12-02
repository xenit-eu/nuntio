package be.vbgn.nuntio.integration.actuators;

import be.vbgn.nuntio.engine.EngineProperties;
import be.vbgn.nuntio.engine.EngineProperties.ShutdownMode;
import java.util.Collections;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;

@Endpoint(
    id = "set-shutdown"
)
@AllArgsConstructor
@Slf4j
public class SetShutdownEndpoint {
    private EngineProperties engineProperties;

    @WriteOperation
    public Map<String, String> setShutdown(ShutdownMode shutdownMode) {
        log.info("Setting shutdown mode to {}", shutdownMode);
        engineProperties.setShutdownMode(shutdownMode);
        return Collections.emptyMap();
    }
}
