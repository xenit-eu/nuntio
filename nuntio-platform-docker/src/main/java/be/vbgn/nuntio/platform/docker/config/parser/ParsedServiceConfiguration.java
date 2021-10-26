package be.vbgn.nuntio.platform.docker.config.parser;

import be.vbgn.nuntio.api.platform.ServiceBinding;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Value;

@Value
public class ParsedServiceConfiguration {

    ConfigurationKind configurationKind;
    ServiceBinding binding;
    String additional;

    public enum ConfigurationKind {
        SERVICE,
        TAGS,
        METADATA
    }

}
