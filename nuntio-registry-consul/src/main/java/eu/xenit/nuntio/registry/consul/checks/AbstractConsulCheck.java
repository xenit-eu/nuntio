package eu.xenit.nuntio.registry.consul.checks;

import com.ecwid.consul.v1.agent.model.NewCheck;
import eu.xenit.nuntio.api.registry.RegistryServiceDescription;
import eu.xenit.nuntio.registry.consul.ConsulServiceIdentifier;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public abstract class AbstractConsulCheck implements ConsulCheck {
    private String id;

    enum CheckStatus {
        WARNING,
        PASSING,
        CRITICAL;

        public static CheckStatus ofStatus(String status) {
            return CheckStatus.valueOf(status.toUpperCase(Locale.ROOT));
        }
    }

    private CheckStatus initialCheckStatus;
    private String deregisterCriticalServiceAfter;

    @Override
    public NewCheck createCheck(ConsulServiceIdentifier serviceIdentifier,
            RegistryServiceDescription serviceDescription) {
        NewCheck check = new NewCheck();
        check.setServiceId(serviceIdentifier.getServiceId());
        check.setId(serviceIdentifier.getServiceIdentifier().withParts(id).toMachineString());
        check.setName(id);
        check.setStatus(initialCheckStatus.name().toLowerCase(Locale.ROOT));
        check.setDeregisterCriticalServiceAfter(deregisterCriticalServiceAfter);
        IpAndPort host = IpAndPort.of(serviceDescription.getAddress().orElse("0.0.0.0"), serviceDescription.getPort());
        initializeCheck(serviceIdentifier, serviceDescription, host, check);
        return check;
    }

    protected abstract void initializeCheck(ConsulServiceIdentifier serviceIdentifier, RegistryServiceDescription serviceDescription, IpAndPort host, NewCheck check);

    abstract static class AbstractCheckFactory<T extends AbstractConsulCheck> implements ConsulCheck.ConsulCheckFactory {

        public static final String INITIAL_STATUS = "initial-status";
        public static final String DEREGISTER_CRITICAL_SERVICE_AFTER = "deregister-critical-service-after";

        @Override
        public T createCheck(String type, String id, Map<String, String> options) {
            if(!supportsCheckType(type)) {
                throw new IllegalArgumentException("Type "+type+" is not supported by this factory.");
            }
            T check = createCheck();
            check.setId(id);
            check.setInitialCheckStatus(CheckStatus.ofStatus(options.getOrDefault(INITIAL_STATUS, CheckStatus.CRITICAL.toString())));
            check.setDeregisterCriticalServiceAfter(options.get(DEREGISTER_CRITICAL_SERVICE_AFTER));
            initializeCheck(check, options);

            Set<String> configuredOptions = new HashSet<>(options.keySet());
            configuredOptions.removeAll(supportedOptions());
            if(!configuredOptions.isEmpty()) {
                throw new IllegalArgumentException("Type "+ type+" does not support options "+configuredOptions);
            }

            return check;
        }

        protected Set<String> supportedOptions() {
            return Set.of(INITIAL_STATUS, DEREGISTER_CRITICAL_SERVICE_AFTER);
        }

        protected abstract T createCheck();

        protected abstract void initializeCheck(T check, Map<String, String> options);
    }
}
