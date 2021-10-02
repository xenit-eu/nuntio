package be.vbgn.nuntio.platform.docker.config.parser;

import com.github.dockerjava.api.command.InspectContainerResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class InspectContainerMetadata implements ContainerMetadata {

    private final InspectContainerResponse inspectContainerResponse;

    public Map<String, String> getEnvironment() {
        String[] environmentVarsStr = inspectContainerResponse.getConfig().getEnv();
        if (environmentVarsStr != null) {
            Map<String, String> environmentVars = new HashMap<>();
            environmentVars = new HashMap<>(environmentVarsStr.length);
            for (String environmentVar : environmentVarsStr) {
                String[] parts = environmentVar.split("=", 2);
                environmentVars.put(parts[0], parts[1]);
            }
            return environmentVars;
        }
        return Collections.emptyMap();
    }

    @Override
    public Map<String, String> getLabels() {
        return inspectContainerResponse.getConfig().getLabels();
    }
}