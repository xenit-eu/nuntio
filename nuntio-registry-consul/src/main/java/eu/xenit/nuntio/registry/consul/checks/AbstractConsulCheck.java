package eu.xenit.nuntio.registry.consul.checks;

import com.ecwid.consul.v1.agent.model.NewCheck;
import eu.xenit.nuntio.api.registry.RegistryServiceDescription;
import eu.xenit.nuntio.registry.consul.ConsulServiceIdentifier;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;
import lombok.experimental.NonFinal;

@Data
@NoArgsConstructor
public abstract class AbstractConsulCheck implements ConsulCheck {
    private String id;

    @Override
    public NewCheck createCheck(ConsulServiceIdentifier serviceIdentifier,
            RegistryServiceDescription serviceDescription) {
        NewCheck check = new NewCheck();
        check.setServiceId(serviceIdentifier.getServiceId());
        check.setId(serviceIdentifier.getServiceIdentifier().withParts(id).toMachineString());
        check.setName(id);
        IpAndPort host = IpAndPort.of(serviceDescription.getAddress().orElse("0.0.0.0"), serviceDescription.getPort());
        initializeCheck(serviceIdentifier, serviceDescription, host, check);
        return check;
    }

    protected abstract void initializeCheck(ConsulServiceIdentifier serviceIdentifier, RegistryServiceDescription serviceDescription, IpAndPort host, NewCheck check);

    abstract static class AbstractCheckFactory<T extends AbstractConsulCheck> implements ConsulCheck.ConsulCheckFactory {

        @Override
        public T createCheck(String type, String id, Map<String, String> options) {
            if(!supportsCheckType(type)) {
                throw new IllegalArgumentException("Type "+type+" is not supported by this factory.");
            }
            T check = createCheck();
            check.setId(id);
            initializeCheck(check, options);

            Set<String> configuredOptions = new HashSet<>(options.keySet());
            configuredOptions.removeAll(supportedOptions());
            if(!configuredOptions.isEmpty()) {
                throw new IllegalArgumentException("Type "+ type+" does not support options "+configuredOptions);
            }

            return check;
        }

        protected abstract Set<String> supportedOptions();

        protected abstract T createCheck();

        protected abstract void initializeCheck(T check, Map<String, String> options);
    }
}
