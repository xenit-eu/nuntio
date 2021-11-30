package be.vbgn.nuntio.integtest;

import be.vbgn.nuntio.integtest.containers.ConsulContainer;
import be.vbgn.nuntio.integtest.containers.DindContainer;
import be.vbgn.nuntio.integtest.util.ConsulWaiter;
import be.vbgn.nuntio.integtest.util.SimpleContainerModifier;
import com.github.dockerjava.api.command.CreateContainerResponse;
import lombok.SneakyThrows;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;

public abstract class ContainerBaseTest {
    protected static Network network = Network.newNetwork();

    @Container
    protected DindContainer dindContainer = new DindContainer()
            .withNetwork(network);
    @Container
    protected ConsulContainer consulContainer = new ConsulContainer()
            .withNetwork(network);

    @SneakyThrows
    protected CreateContainerResponse createContainer(SimpleContainerModifier containerModifier) {
        dindContainer.getDindClient().pullImageCmd("library/alpine")
                .withRegistry("docker.io")
                .withTag("latest")
                .start()
                .awaitCompletion();

        var cmd = dindContainer.getDindClient().createContainerCmd("alpine")
                .withCmd("sleep", "infinity");
        containerModifier.apply(cmd);
        return cmd.exec();
    }

    protected ConsulWaiter consulWaiter() {
        return new ConsulWaiter(consulContainer.getConsulClient());
    }
}
