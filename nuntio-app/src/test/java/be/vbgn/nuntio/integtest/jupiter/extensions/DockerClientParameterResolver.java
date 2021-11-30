package be.vbgn.nuntio.integtest.jupiter.extensions;

import be.vbgn.nuntio.integtest.containers.DindContainer;
import com.github.dockerjava.api.DockerClient;

public class DockerClientParameterResolver extends AbstractClientParameterResolver<DockerClient, DindContainer> {

    public DockerClientParameterResolver() {
        super(DindContainer.class, DockerClient.class, DindContainer::getDindClient);
    }
}
