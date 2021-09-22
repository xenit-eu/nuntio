package be.vbgn.nuntio.platform.docker;

import be.vbgn.nuntio.api.platform.PlatformServiceIdentifier;
import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@NonFinal
public class DockerContainerIdServiceIdentifier implements PlatformServiceIdentifier {

    String containerId;

}
