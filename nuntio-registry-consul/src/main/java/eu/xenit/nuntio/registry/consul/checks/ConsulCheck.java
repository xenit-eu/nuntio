package eu.xenit.nuntio.registry.consul.checks;

import com.ecwid.consul.v1.agent.model.NewCheck;
import eu.xenit.nuntio.api.checks.InvalidCheckException;
import eu.xenit.nuntio.api.checks.ServiceCheck;
import eu.xenit.nuntio.api.checks.ServiceCheckFactory;
import eu.xenit.nuntio.api.registry.RegistryServiceDescription;
import eu.xenit.nuntio.registry.consul.ConsulServiceIdentifier;
import java.util.Map;

public interface ConsulCheck extends ServiceCheck {

    NewCheck createCheck(ConsulServiceIdentifier serviceIdentifier, RegistryServiceDescription serviceDescription);

    interface ConsulCheckFactory extends ServiceCheckFactory {
        @Override
        ConsulCheck createCheck(String type, String id, Map<String, String> options) throws InvalidCheckException;
    }

}
