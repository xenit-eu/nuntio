package be.vbgn.nuntio.integtest.util;

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Ports;
import com.github.dockerjava.api.model.Ports.Binding;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@FunctionalInterface
public interface SimpleContainerModifier {
    void apply(CreateContainerCmd createContainerCmd);

    default SimpleContainerModifier andThen(SimpleContainerModifier after) {
        Objects.requireNonNull(after);
        return (t) -> {
            this.apply(t);
            after.apply(t);
        };
    }

    static SimpleContainerModifier withPortBinding(ExposedPort exposedPort, Binding binding) {
        return AddHostConfig.INSTANCE
                .andThen(createContainerCmd -> {
                    createContainerCmd.withExposedPorts(appendItem(createContainerCmd.getExposedPorts(), exposedPort));
                    createContainerCmd.getHostConfig().getPortBindings().bind(exposedPort, binding);
                });
    }

    static SimpleContainerModifier withEnvVar(String key, String value) {
        return createContainerCmd -> {
            createContainerCmd.withEnv(appendItem(createContainerCmd.getEnv(), key+"="+value));
        };
    }

    private static <T> List<T> appendItem(T[] items, T item) {
        if(items == null) {
            return Collections.singletonList(item);
        }
        var itemsList = new ArrayList<>(Arrays.asList(items));
        itemsList.add(item);
        return itemsList;
    }



    static SimpleContainerModifier withLabel(String key, String value) {
        return createContainerCmd -> {
            if(createContainerCmd.getLabels() == null) {
                createContainerCmd.withLabels(new HashMap<>());
            }
            Map<String, String> originalLabels = createContainerCmd.getLabels();
            var newLabels = new HashMap<>(originalLabels);
            newLabels.put(key, value);
            createContainerCmd.withLabels(newLabels);
        };
    }
}


class AddHostConfig implements SimpleContainerModifier {
    static final SimpleContainerModifier INSTANCE = new AddHostConfig();

    private AddHostConfig() {

    }

    @Override
    public void apply(CreateContainerCmd createContainerCmd) {
        if(createContainerCmd.getHostConfig() == null) {
            createContainerCmd.withHostConfig(HostConfig.newHostConfig());
        }

        if(createContainerCmd.getHostConfig().getPortBindings() == null) {
            createContainerCmd.getHostConfig().withPortBindings(new Ports());
        }
    }
}
