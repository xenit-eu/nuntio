package be.vbgn.nuntio.core.service;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.NonFinal;

@Value
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ServiceConfiguration {

    ServiceBinding binding;
    @ToString.Exclude
    ServiceBinding originalBinding;
    Set<String> services;
    Set<String> tags;
    Map<String, String> metadata;

    public ServiceConfiguration(ServiceBinding binding, Set<String> services, Set<String> tags,
            Map<String, String> metadata) {
        this(binding, binding, services, tags, metadata);

    }

    public ServiceConfiguration withBinding(ServiceBinding serviceBinding) {
        return new ServiceConfiguration(serviceBinding,
                originalBinding == ServiceBinding.ANY ? serviceBinding : originalBinding, services, tags, metadata);
    }

    @Value
    @NonFinal
    public static class ServiceBinding {

        String ip;
        Integer port;
        String protocol;

        private static final String DEFAULT_PROTOCOL = "tcp";

        public static final ServiceBinding ANY = new ServiceBinding(null, null, null) {
            @Override
            public String toString() {
                String parentToString = super.toString();
                return parentToString.substring(0, parentToString.indexOf('(')) + "(ANY)";
            }
        };

        @Override
        public String toString() {
            String portString = Objects.toString(port) + "/" + Objects.toString(protocol);
            if (ip == null) {
                return "ServiceBinding(port=" + portString + ")";
            } else {
                return "ServiceBinding(ip=" + Objects.toString(ip) + ", port=" + portString + ")";
            }
        }

    }
}
