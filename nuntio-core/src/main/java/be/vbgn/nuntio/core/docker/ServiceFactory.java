package be.vbgn.nuntio.core.docker;

import be.vbgn.nuntio.core.service.Service;
import be.vbgn.nuntio.core.service.ServiceConfiguration;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor(onConstructor_ = {@Autowired})
public class ServiceFactory {

    public Set<Service> createFromConfiguration(Service.Identifier identifier, ServiceConfiguration configuration) {
        return configuration.getServices().stream()
                .map(serviceName -> createService(identifier, serviceName, configuration))
                .collect(Collectors.toSet());
    }

    private Service createService(Service.Identifier identifier, String name, ServiceConfiguration configuration) {
        return new Service(identifier, name, configuration.getBinding(), configuration.getOriginalBinding(),
                configuration.getTags(),
                configuration.getMetadata());
    }
}
