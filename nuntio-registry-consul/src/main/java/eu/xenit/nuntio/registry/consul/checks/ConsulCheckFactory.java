package eu.xenit.nuntio.registry.consul.checks;

import eu.xenit.nuntio.api.checks.ServiceCheck;
import eu.xenit.nuntio.api.checks.ServiceCheckFactory;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ConsulCheckFactory implements ServiceCheckFactory {

    private final List<ConsulCheck.ConsulCheckFactory> factories;

    private Optional<ConsulCheck.ConsulCheckFactory> findFactoryFor(String type) {
        for (ConsulCheck.ConsulCheckFactory factory : factories) {
            if(factory.supportsCheckType(type)) {
                return Optional.of(factory);
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean supportsCheckType(String type) {
        return findFactoryFor(type).isPresent();
    }

    @Override
    public ServiceCheck createCheck(String type, String id, Map<String, String> options) {
        return findFactoryFor(type)
                .map(factory -> factory.createCheck(type, id, options))
                .orElseThrow(() -> new IllegalArgumentException("Check type "+type+" is not supported."));
    }
}
