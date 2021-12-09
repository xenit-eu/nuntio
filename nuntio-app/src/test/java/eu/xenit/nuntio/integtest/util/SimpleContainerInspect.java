package eu.xenit.nuntio.integtest.util;

import com.github.dockerjava.api.command.InspectContainerCmd;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports.Binding;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

public class SimpleContainerInspect {
    private final InspectContainerResponse inspectContainerResponse;

    public SimpleContainerInspect(InspectContainerCmd inspectContainerCmd) {
        this(inspectContainerCmd.exec());
    }

    public SimpleContainerInspect(InspectContainerResponse inspectContainerResponse) {
        this.inspectContainerResponse = inspectContainerResponse;
    }

    public Map<String, String> findInternalIps() {
        return inspectContainerResponse.getNetworkSettings().getNetworks().entrySet()
                .stream()
                .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().getIpAddress()));
    }

    public Map<ExposedPort, Binding[]> getContainerBindings() {
        return inspectContainerResponse.getNetworkSettings().getPorts()
                .getBindings();
    }

    public Binding[] getContainerBindings(ExposedPort exposedPort) {
        var result = getContainerBindings().get(exposedPort);
        if(result == null) {
            return new Binding[0];
        }
        return result;
    }

    public Binding findSingleContainerBinding(ExposedPort exposedPort) {
        var bindings = getContainerBindings(exposedPort);
        switch (bindings.length) {
            case 0:
                throw new IllegalStateException("No bindings found for container "+inspectContainerResponse.getId()+" and port "+exposedPort);
            case 1:
                return bindings[0];
            default:
                throw new IllegalStateException("Multiple bindings found for container "+inspectContainerResponse.getId()+" and port "+exposedPort);
        }
    }

    public Integer findSingleContainerPort(ExposedPort exposedPort) {
        var binding = findSingleContainerBinding(exposedPort);
        return Integer.parseInt(binding.getHostPortSpec());
    }

}
