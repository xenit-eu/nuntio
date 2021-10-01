package be.vbgn.nuntio.registry.consul;

import be.vbgn.nuntio.api.SharedIdentifier;
import be.vbgn.nuntio.api.registry.CheckStatus;
import be.vbgn.nuntio.api.registry.CheckType;
import be.vbgn.nuntio.api.registry.RegistryServiceDescription;
import be.vbgn.nuntio.api.registry.RegistryServiceIdentifier;
import be.vbgn.nuntio.api.registry.ServiceRegistry;
import be.vbgn.nuntio.registry.consul.ConsulProperties.CheckProperties;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.agent.model.NewCheck;
import com.ecwid.consul.v1.agent.model.NewService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class ConsulRegistry implements ServiceRegistry {

    private static final String NUNTIO_SID = "nuntio-sid";
    private ConsulClient consulClient;
    private ConsulProperties consulConfig;

    @Override
    public Set<? extends RegistryServiceIdentifier> findServices() {
        return consulClient.getAgentServices()
                .getValue()
                .values()
                .stream()
                .filter(service -> service.getMeta().containsKey(NUNTIO_SID))
                .map(service -> new ConsulServiceIdentifier(service.getService(), service.getId(),
                        SharedIdentifier.parse(service.getMeta().get(NUNTIO_SID))))
                .collect(Collectors.toSet());
    }

    @Override
    public RegistryServiceIdentifier registerService(RegistryServiceDescription description) {
        log.debug("Registering service {}", description);

        NewService newService = new NewService();

        ConsulServiceIdentifier consulServiceIdentifier = ConsulServiceIdentifier.fromDescription(description);

        newService.setName(consulServiceIdentifier.getServiceName());
        newService.setId(consulServiceIdentifier.getServiceId());

        description.getAddress().ifPresent(newService::setAddress);
        newService.setPort(Integer.parseInt(description.getPort()));
        newService.setTags(new ArrayList<>(description.getTags()));
        Map<String, String> metadata = new HashMap<>(description.getMetadata());

        metadata.put(NUNTIO_SID, description.getSharedIdentifier().toMachineString());

        newService.setMeta(metadata);

        log.trace("Consul service object: {}", newService);
        consulClient.agentServiceRegister(newService, consulConfig.getToken());
        log.debug("Registered service {}", description);

        return consulServiceIdentifier;
    }

    @Override
    public void unregisterService(RegistryServiceIdentifier serviceIdentifier) {
        if (serviceIdentifier instanceof ConsulServiceIdentifier) {
            log.debug("Deregistering service {}", serviceIdentifier);
            var consulServiceIdentifier = (ConsulServiceIdentifier) serviceIdentifier;
            consulClient.agentServiceDeregister(consulServiceIdentifier.getServiceId(), consulConfig.getToken());
            log.debug("Deregistered service {}", serviceIdentifier);
        }
    }

    private static String createCheckId(ConsulServiceIdentifier registryServiceIdentifier, CheckType checkType) {
        return registryServiceIdentifier.getSharedIdentifier()
                .withParts(registryServiceIdentifier.getServiceId(), checkType.getCheckId())
                .toMachineString();
    }

    @Override
    public void registerCheck(RegistryServiceIdentifier serviceIdentifier, CheckType checkType) {
        if (serviceIdentifier instanceof ConsulServiceIdentifier) {
            log.debug("Registering check {}:{}", serviceIdentifier, checkType);
            var consulServiceIdentifier = (ConsulServiceIdentifier) serviceIdentifier;
            NewCheck newCheck = new NewCheck();

            newCheck.setId(createCheckId(consulServiceIdentifier, checkType));
            newCheck.setServiceId(consulServiceIdentifier.getServiceId());
            newCheck.setName(checkType.getTitle());
            newCheck.setNotes(checkType.getDescription());

            Optional<CheckProperties> checkConfig = Optional.ofNullable(consulConfig.getChecks().get(checkType));
            checkConfig.map(CheckProperties::getTtl).ifPresent(newCheck::setTtl);
            checkConfig.map(CheckProperties::getDeregisterCriticalServiceAfter)
                    .ifPresent(newCheck::setDeregisterCriticalServiceAfter);

            log.trace("Consul check object: {}", newCheck);
            consulClient.agentCheckRegister(newCheck, consulConfig.getToken());
            log.debug("Registered check {}:{}", serviceIdentifier, checkType);
        }
    }

    @Override
    public void unregisterCheck(RegistryServiceIdentifier serviceIdentifier, CheckType checkType) {
        if (serviceIdentifier instanceof ConsulServiceIdentifier) {
            log.debug("Deregistering check {}:{}", serviceIdentifier, checkType);
            consulClient.agentCheckDeregister(createCheckId((ConsulServiceIdentifier) serviceIdentifier, checkType),
                    consulConfig.getToken());
            log.debug("Deregistered check {}:{}", serviceIdentifier, checkType);
        }
    }

    @Override
    public void updateCheck(RegistryServiceIdentifier serviceIdentifier, CheckType checkType, CheckStatus checkStatus,
            String message) {
        if (serviceIdentifier instanceof ConsulServiceIdentifier) {
            log.debug("Updating check {}:{} with {}", serviceIdentifier, checkType, checkStatus);
            registerCheck(serviceIdentifier, checkType);

            String checkId = createCheckId((ConsulServiceIdentifier) serviceIdentifier, checkType);

            switch (checkStatus) {
                case FAILING:
                    consulClient.agentCheckFail(checkId, message, consulConfig.getToken());
                    break;
                case WARNING:
                    consulClient.agentCheckWarn(checkId, message, consulConfig.getToken());
                    break;
                case PASSING:
                    consulClient.agentCheckPass(checkId, message, consulConfig.getToken());
                    break;
            }
            log.debug("Updated check {}:{} with {}", serviceIdentifier, checkType, checkStatus);
        }
    }
}
