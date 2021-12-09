package eu.xenit.nuntio.integtest.jupiter.extensions;

import eu.xenit.nuntio.integtest.containers.DindContainer;
import com.github.dockerjava.api.DockerClient;

public class DockerClientParameterResolver extends AbstractClientParameterResolver<DockerClient, DindContainer> {

    public DockerClientParameterResolver() {
        super(DindContainer.class, DockerClient.class, DindContainer::getDindClient);
    }
}
