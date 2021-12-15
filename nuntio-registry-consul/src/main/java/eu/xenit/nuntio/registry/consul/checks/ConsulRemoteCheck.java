package eu.xenit.nuntio.registry.consul.checks;

import com.ecwid.consul.v1.agent.model.NewCheck;
import eu.xenit.nuntio.api.registry.RegistryServiceDescription;
import eu.xenit.nuntio.registry.consul.ConsulServiceIdentifier;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.NonFinal;


@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public abstract class ConsulRemoteCheck extends AbstractConsulCheck {
    private String interval;
    private String timeout;

    @Override
    public NewCheck createCheck(
            ConsulServiceIdentifier serviceIdentifier, RegistryServiceDescription registryServiceDescription) {
        NewCheck check = super.createCheck(serviceIdentifier, registryServiceDescription);
        check.setInterval(interval);
        check.setTimeout(timeout);
        return check;
    }

    abstract static class RemoteCheckFactory<T extends ConsulRemoteCheck> extends AbstractCheckFactory<T> {

        @Override
        protected void initializeCheck(T check, Map<String, String> options) {
            check.setInterval(options.get("interval"));
            check.setTimeout(options.get("timeout"));
        }

        @Override
        protected Set<String> supportedOptions() {
            return Set.of("interval", "timeout");
        }
    }

}

