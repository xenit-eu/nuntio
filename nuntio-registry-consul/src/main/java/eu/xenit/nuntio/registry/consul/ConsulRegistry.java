package eu.xenit.nuntio.registry.consul;

import com.ecwid.consul.v1.agent.model.Service;
import eu.xenit.nuntio.api.identifier.PlatformIdentifier;
import eu.xenit.nuntio.api.identifier.ServiceIdentifier;
import eu.xenit.nuntio.api.registry.CheckStatus;
import eu.xenit.nuntio.api.registry.CheckType;
import eu.xenit.nuntio.api.registry.RegistryServiceDescription;
import eu.xenit.nuntio.api.registry.RegistryServiceIdentifier;
import eu.xenit.nuntio.api.registry.ServiceRegistry;
import eu.xenit.nuntio.api.registry.metrics.RegistryMetrics;
import eu.xenit.nuntio.registry.consul.ConsulProperties.CheckProperties;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.agent.model.NewCheck;
import com.ecwid.consul.v1.agent.model.NewService;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
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
    private RegistryMetrics registryMetrics;

    private Optional<ConsulServiceIdentifier> mapConsulServiceToServiceIdentifier(Service service) {
        return Optional.ofNullable(service.getMeta().get(NUNTIO_SID))
                .map(ServiceIdentifier::parse)
                .map(sid -> new ConsulServiceIdentifier(service.getService(), service.getId(), sid));
    }

    @Override
    public Set<? extends RegistryServiceIdentifier> findServices() {
        return registryMetrics.findServices(() -> consulClient.getAgentServices()
                .getValue()
                .values()
                .stream()
                .map(this::mapConsulServiceToServiceIdentifier)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet())
        );
    }

    @Override
    public Map<? extends RegistryServiceIdentifier, RegistryServiceDescription> findServiceDescriptions(PlatformIdentifier sharedIdentifier) {
        return findServiceDescriptions()
                .entrySet()
                .stream()
                .filter(service -> Objects.equals(service.getKey().getPlatformIdentifier(), sharedIdentifier))
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    public Map<? extends RegistryServiceIdentifier, RegistryServiceDescription> findServiceDescriptions() {
        return registryMetrics.findServiceDescriptions(() -> consulClient.getAgentServices()
                .getValue()
                .values()
                .stream()
                .flatMap(consulService -> mapConsulServiceToServiceIdentifier(consulService)
                        .map(sid -> Map.entry(sid, new RegistryServiceDescription(
                                sid.getServiceIdentifier(),
                                sid.getPlatformIdentifier(),
                                consulService.getService(),
                                Optional.ofNullable(consulService.getAddress()),
                                Integer.toString(consulService.getPort()),
                                new HashSet<>(consulService.getTags()),
                                consulService.getMeta()
                        )))
                        .stream())
                .collect(Collectors.toUnmodifiableMap(Entry::getKey, Entry::getValue))
        );
    }

    @Override
    public Optional<RegistryServiceDescription> findServiceDescription(RegistryServiceIdentifier serviceIdentifier) {
        if(serviceIdentifier instanceof ConsulServiceIdentifier) {
            return Optional.ofNullable(findServiceDescriptions().get(serviceIdentifier));
        } else {
            return Optional.empty();
        }
    }

    private String extractServiceAddress(RegistryServiceDescription description) {
        return description.getAddress()
                .filter(address -> {
                    try {
                        if(InetAddress.getByName(address).isAnyLocalAddress()) {
                            return false;
                        }
                    } catch (UnknownHostException e) {
                        log.error("Failed to determine if service {} address {} is a local address", description, address, e);
                        return true;
                    }
                    return true;
                })
                .orElseGet(() -> consulClient.getAgentSelf().getValue().getMember().getAddress());
    }

    @Override
    public RegistryServiceIdentifier registerService(RegistryServiceDescription description) {
        return registryMetrics.registerService(() -> {
            log.debug("Registering service {}", description);

            NewService newService = new NewService();

            ConsulServiceIdentifier consulServiceIdentifier = ConsulServiceIdentifier.fromDescription(description);

            newService.setName(consulServiceIdentifier.getServiceName());
            newService.setId(consulServiceIdentifier.getServiceId());

            newService.setAddress(extractServiceAddress(description));
            newService.setPort(Integer.parseInt(description.getPort()));
            newService.setTags(new ArrayList<>(description.getTags()));
            Map<String, String> metadata = new HashMap<>(description.getMetadata());

            metadata.put(NUNTIO_SID, description.getServiceIdentifier().toMachineString());

            newService.setMeta(metadata);

            log.trace("Consul service object: {}", newService);
            consulClient.agentServiceRegister(newService, consulConfig.getToken());
            log.debug("Registered service {}", description);

            return consulServiceIdentifier;
        });
    }

    @Override
    public void unregisterService(RegistryServiceIdentifier serviceIdentifier) {
        if (serviceIdentifier instanceof ConsulServiceIdentifier) {
            registryMetrics.unregisterService(() -> {
                log.debug("Deregistering service {}", serviceIdentifier);
                var consulServiceIdentifier = (ConsulServiceIdentifier) serviceIdentifier;
                consulClient.agentServiceDeregister(consulServiceIdentifier.getServiceId(), consulConfig.getToken());
                log.debug("Deregistered service {}", serviceIdentifier);
            });
        }
    }

    private static String createCheckId(ConsulServiceIdentifier registryServiceIdentifier, CheckType checkType) {
        return registryServiceIdentifier.getServiceIdentifier()
                .withParts(checkType.getCheckId())
                .toMachineString();
    }

    @Override
    public void registerCheck(RegistryServiceIdentifier serviceIdentifier, CheckType checkType) {
        if (serviceIdentifier instanceof ConsulServiceIdentifier) {
            registryMetrics.registerCheck(() -> {
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
            });
        }
    }

    @Override
    public void unregisterCheck(RegistryServiceIdentifier serviceIdentifier, CheckType checkType) {
        if (serviceIdentifier instanceof ConsulServiceIdentifier) {
            registryMetrics.unregisterCheck(() -> {
                if(checkExists((ConsulServiceIdentifier) serviceIdentifier, checkType)) {
                    log.debug("Deregistering check {}:{}", serviceIdentifier, checkType);
                    consulClient.agentCheckDeregister(
                            createCheckId((ConsulServiceIdentifier) serviceIdentifier, checkType),
                            consulConfig.getToken());
                    log.debug("Deregistered check {}:{}", serviceIdentifier, checkType);
                }

            });
        }
    }

    @Override
    public void updateCheck(RegistryServiceIdentifier serviceIdentifier, CheckType checkType, CheckStatus checkStatus,
            String message) {
        if (serviceIdentifier instanceof ConsulServiceIdentifier) {
            registryMetrics.updateCheck(() -> {
                log.debug("Updating check {}:{} with {}", serviceIdentifier, checkType, checkStatus);

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
            });
        }
    }

    private boolean checkExists(ConsulServiceIdentifier serviceIdentifier, CheckType checkType) {
        String checkId = createCheckId(serviceIdentifier, checkType);
        var agentChecks = consulClient.getAgentChecks().getValue();
        return agentChecks.containsKey(checkId);
    }
}
