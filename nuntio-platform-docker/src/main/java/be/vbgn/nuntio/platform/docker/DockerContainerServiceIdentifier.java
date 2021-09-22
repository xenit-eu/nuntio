package be.vbgn.nuntio.platform.docker;

import lombok.EqualsAndHashCode;
import lombok.Value;


@EqualsAndHashCode(callSuper = true)
@Value
public class DockerContainerServiceIdentifier extends DockerContainerIdServiceIdentifier {

    String containerName;

    public DockerContainerServiceIdentifier(String containerName, String containerId) {
        super(containerId);
        this.containerName = containerName;
    }

}
