package be.vbgn.nuntio.core.service;

import be.vbgn.nuntio.core.service.ServiceConfiguration.ServiceBinding;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.ToString;
import lombok.Value;

@Value
public class Service {

    @Value
    public static class Identifier {

        private static final Pattern SID_PATTERN = Pattern.compile(
                "^nuntio-sid-(?<containerId>[0-9a-f]+)-(?<containerName>.+)$");

        String containerName;
        String containerId;

        public String toSid() {
            return ("nuntio-sid-" + containerId + "-" + containerName);
        }

        public static Optional<Identifier> fromSid(String sid) {
            if (sid == null) {
                return Optional.empty();
            }

            Matcher matcher = SID_PATTERN.matcher(sid);
            if (matcher.matches()) {
                return Optional.of(new Identifier(matcher.group("containerName"), matcher.group("containerId")));
            }
            return Optional.empty();
        }
    }


    Identifier serviceIdentifier;
    String name;

    ServiceBinding binding;
    @ToString.Exclude
    ServiceBinding originalBinding;

    Set<String> tags;
    Map<String, String> metadata;
}
