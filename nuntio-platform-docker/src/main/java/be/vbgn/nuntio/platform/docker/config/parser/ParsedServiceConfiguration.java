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

    @AllArgsConstructor
    public enum ConfigurationKind {
        SERVICE("service", false),
        TAGS("tags", false),
        METADATA("metadata", true),
        ;

        @Getter(value = AccessLevel.PACKAGE)
        private final String identifier;

        @Getter(value = AccessLevel.PACKAGE)
        private final boolean acceptsAdditional;

        static Optional<ConfigurationKind> find(String identifier) {
            for (ConfigurationKind configurationKind : values()) {
                if (configurationKind.getIdentifier().equals(identifier)) {
                    return Optional.of(configurationKind);
                }
            }
            return Optional.empty();
        }
    }
}
