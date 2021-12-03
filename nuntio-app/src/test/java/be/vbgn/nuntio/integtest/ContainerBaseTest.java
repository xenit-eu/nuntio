package be.vbgn.nuntio.integtest;

import static org.awaitility.Awaitility.await;

import be.vbgn.nuntio.integtest.containers.ConsulContainer;
import be.vbgn.nuntio.integtest.containers.DindContainer;
import be.vbgn.nuntio.integtest.util.ConsulWaiter;
import be.vbgn.nuntio.integtest.util.SimpleContainerModifier;
import com.github.dockerjava.api.async.ResultCallback.Adapter;
import com.github.dockerjava.api.command.CreateContainerResponse;
import com.github.dockerjava.api.model.PullResponseItem;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.core.ConditionFactory;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

@Slf4j
public abstract class ContainerBaseTest {
    protected static Network network = Network.newNetwork();
    protected static Set<String> pulledImages = new HashSet<>();

    @Container
    protected DindContainer dindContainer = new DindContainer()
            .withNetwork(network);
    @Container
    protected ConsulContainer consulContainer = new ConsulContainer()
            .withNetwork(network);

    protected static final ConditionFactory await = await().timeout(2, TimeUnit.MINUTES);

    @SneakyThrows
    protected CreateContainerResponse createContainer(String imageId, SimpleContainerModifier containerModifier) {
        DockerImageName dockerImageName = DockerImageName.parse(imageId);
        if(!pulledImages.contains(dockerImageName.asCanonicalNameString())) {
            var pullImage = dindContainer.getDindClient().pullImageCmd(dockerImageName.getRepository())
                    .withRegistry(dockerImageName.getRegistry())
                    .withTag(dockerImageName.getVersionPart())
                    .exec(new Adapter<>() {
                        @Override
                        public void onNext(PullResponseItem object) {
                            if (object.getProgress() != null) {
                                log.debug("{} {}", object.getStatus(), object.getProgress());
                            } else if (object.getError() != null) {
                                log.error("{}", object.getError());
                            } else {
                                log.info("{}", object.getStatus());
                            }
                        }
                    });

            pullImage.awaitCompletion();
            pulledImages.add(dockerImageName.asCanonicalNameString());
        } else {
            log.info("Image {} was already pulled. Not pulling it again.", dockerImageName.asCanonicalNameString());
        }

        var cmd = dindContainer.getDindClient().createContainerCmd(dockerImageName.asCanonicalNameString())
                .withCmd("sleep", "infinity");
        containerModifier.apply(cmd);
        return cmd.exec();
    }

    protected CreateContainerResponse createContainer(SimpleContainerModifier containerModifier) {
        return createContainer("library/alpine", containerModifier);
    }

    protected ConsulWaiter consulWaiter() {
        return new ConsulWaiter(consulContainer.getConsulClient());
    }
}
