package eu.xenit.nuntio.registry.consul.checks;

import com.ecwid.consul.v1.agent.model.NewCheck;
import eu.xenit.nuntio.api.registry.RegistryServiceDescription;
import eu.xenit.nuntio.registry.consul.ConsulServiceIdentifier;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ConsulHttpCheck extends ConsulRemoteCheck
{
    @AllArgsConstructor
    @Getter
    public enum HttpCheckScheme {
        HTTP("http"),
        HTTPS("https");

        private final String scheme;

        public static final HttpCheckScheme ofScheme(String scheme) {
            for (HttpCheckScheme value : values()) {
                if(Objects.equals(value.getScheme(), scheme.toLowerCase(Locale.ROOT))) {
                    return value;
                }
            }
            throw new IllegalArgumentException("Invalid scheme "+scheme);
        }
    }

    private String method;
    private HttpCheckScheme scheme;
    private String path;
    private Map<String, List<String>> headers;

    @Override
    protected void initializeCheck(ConsulServiceIdentifier serviceIdentifier,
            RegistryServiceDescription serviceDescription, IpAndPort host, NewCheck check) {
        check.setMethod(method);
        check.setHeader(headers);
        check.setHttp(scheme.getScheme()+"://"+host.toHost()+"/"+ path);
        check.setNotes(check.getMethod()+" "+check.getHttp());
    }

    public static class HttpCheckFactory extends RemoteCheckFactory<ConsulHttpCheck> {
        private static final Set<String> CHECK_TYPES = Set.of("consul:http", "http");

        @Override
        public boolean supportsCheckType(String type) {
            return CHECK_TYPES.contains(type);
        }

        @Override
        protected ConsulHttpCheck createCheck() {
            return new ConsulHttpCheck();
        }

        @Override
        protected void initializeCheck(ConsulHttpCheck check, Map<String, String> options) {
            super.initializeCheck(check, options);
            check.setMethod(options.getOrDefault("method", "GET"));
            check.setPath(options.getOrDefault("path", "/"));
            check.setScheme(HttpCheckScheme.ofScheme(options.getOrDefault("scheme", HttpCheckScheme.HTTP.getScheme())));
        }

        @Override
        protected Set<String> supportedOptions() {
            Set<String> supportedOptions = new HashSet<>(super.supportedOptions());
            supportedOptions.add("method");
            supportedOptions.add("path");
            supportedOptions.add("scheme");
            return supportedOptions;
        }
    }

}
