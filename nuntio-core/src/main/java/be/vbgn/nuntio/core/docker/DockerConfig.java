package be.vbgn.nuntio.core.docker;

import lombok.Value;
import lombok.experimental.NonFinal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

@Value
@ConstructorBinding
@ConfigurationProperties("nuntio.docker")
@NonFinal
public class DockerConfig {

    String labelPrefix;

    boolean internal;
}
