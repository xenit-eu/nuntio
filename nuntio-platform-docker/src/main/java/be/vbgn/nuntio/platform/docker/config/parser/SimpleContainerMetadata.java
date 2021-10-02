package be.vbgn.nuntio.platform.docker.config.parser;

import java.util.Map;
import lombok.Value;

@Value
public class SimpleContainerMetadata implements ContainerMetadata{
    Map<String, String> environment;
    Map<String, String> labels;
}
