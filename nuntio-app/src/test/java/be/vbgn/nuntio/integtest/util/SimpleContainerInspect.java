package be.vbgn.nuntio.integtest.util;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.Ports.Binding;
import java.util.Map;

public class SimpleContainerInspect {
    private final InspectContainerResponse inspectContainerResponse;

    public SimpleContainerInspect(InspectContainerResponse inspectContainerResponse) {
        this.inspectContainerResponse = inspectContainerResponse;
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

}
