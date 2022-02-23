package eu.xenit.nuntio.platform.docker.config.parser;

import eu.xenit.nuntio.api.platform.ServiceBinding;
import com.github.dockerjava.api.command.InspectContainerResponse;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class InspectContainerMetadata implements ContainerMetadata {

    private final InspectContainerResponse inspectContainerResponse;

    @Override
    public String getImageName() {
        return inspectContainerResponse.getConfig().getImage();
    }

    @Override
    public Set<ServiceBinding> getInternalPortBindings() {
        return inspectContainerResponse.getNetworkSettings().getPorts()
                .getBindings()
                .keySet()
                .stream()
                .map(exposedPort -> ServiceBinding.fromPortAndProtocol(exposedPort.getPort(), exposedPort.getProtocol().toString()))
                .collect(Collectors.toSet());
    }

    public Map<String, String> getEnvironment() {
        String[] environmentVarsStr = inspectContainerResponse.getConfig().getEnv();
        if (environmentVarsStr != null) {
            Map<String, String> environmentVars = new HashMap<>();
            environmentVars = new HashMap<>(environmentVarsStr.length);
            for (String environmentVar : environmentVarsStr) {
                String[] parts = environmentVar.split("=", 2);
                if(parts.length == 2) {
                    environmentVars.put(parts[0], parts[1]);
                }
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
