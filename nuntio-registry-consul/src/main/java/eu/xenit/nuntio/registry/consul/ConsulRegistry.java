package eu.xenit.nuntio.registry.consul;

import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.OperationException;
import com.ecwid.consul.v1.agent.model.NewCheck;
import com.ecwid.consul.v1.agent.model.NewService;
import eu.xenit.nuntio.api.identifier.ServiceIdentifier;
import eu.xenit.nuntio.api.registry.CheckStatus;
import eu.xenit.nuntio.api.registry.CheckType;
import eu.xenit.nuntio.api.registry.RegistryServiceDescription;
import eu.xenit.nuntio.api.registry.RegistryServiceIdentifier;
import eu.xenit.nuntio.api.registry.ServiceRegistry;
import eu.xenit.nuntio.api.registry.errors.ServiceDeregistrationException;
import eu.xenit.nuntio.api.registry.errors.ServiceOperationException;
import eu.xenit.nuntio.api.registry.errors.ServiceRegistrationException;
import eu.xenit.nuntio.api.registry.metrics.RegistryMetrics;
import eu.xenit.nuntio.registry.consul.ConsulProperties.CheckProperties;
import eu.xenit.nuntio.registry.consul.checks.ConsulCheck;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
    private RegistryMetrics registryMetrics;

    @Override
    public Set<? extends RegistryServiceIdentifier> findServices() {
        return registryMetrics.findServices(() ->consulClient.getAgentServices()
                .getValue()
                .values()
                .stream()
                .filter(service -> service.getMeta().containsKey(NUNTIO_SID))
                .map(service -> new ConsulServiceIdentifier(service.getService(), service.getId(),
                        ServiceIdentifier.parse(service.getMeta().get(NUNTIO_SID))
                ))
                .collect(Collectors.toSet())
        );
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
    public RegistryServiceIdentifier registerService(RegistryServiceDescription description) throws ServiceRegistrationException {
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

            try {
                log.trace("Consul service object: {}", newService);
                consulClient.agentServiceRegister(newService, consulConfig.getToken());
                log.debug("Registered service {}", description);
            } catch(OperationException e) {
                throw new ServiceRegistrationException(description, null, e);
            }


            Set<NewCheck> checks = description.getChecks()
                    .stream()
                    .filter(check -> check instanceof ConsulCheck)
                    .map(check -> ((ConsulCheck) check).createCheck(consulServiceIdentifier, description))
                    .collect(Collectors.toSet());
            log.trace("Consul check objects: {}", checks);
            for (NewCheck newCheck : checks) {
                try {
                    log.trace("Registering check {}", newCheck);
                    consulClient.agentCheckRegister(newCheck, consulConfig.getToken());
                    log.debug("Registered check {}", newCheck);
                } catch(OperationException e) {
                    log.error("Failed to register check {}. Unregistering service and failing registration attempt.", newCheck, e);
                    try {
                        unregisterService(consulServiceIdentifier);
                    } catch(ServiceDeregistrationException e2) {
                        log.error("Failed to unregister service {} because of a failed registration attempt.", consulServiceIdentifier, e2);
                    }
                    throw new ServiceRegistrationException(description, "Failed to register check for service.", e);
                }
            }
            log.debug("Registered checks for service {}", description);

            return consulServiceIdentifier;
        });
    }

    @Override
    public void unregisterService(RegistryServiceIdentifier serviceIdentifier) throws ServiceDeregistrationException {
        if (serviceIdentifier instanceof ConsulServiceIdentifier) {
            registryMetrics.unregisterService(() -> {
                log.debug("Deregistering service {}", serviceIdentifier);
                var consulServiceIdentifier = (ConsulServiceIdentifier) serviceIdentifier;
                try {
                    consulClient.agentServiceDeregister(consulServiceIdentifier.getServiceId(),
                            consulConfig.getToken());
                    log.debug("Deregistered service {}", serviceIdentifier);
                } catch(OperationException e) {
                    throw new ServiceDeregistrationException(serviceIdentifier, null, e);
                }
            });
        }
    }

    private static String createCheckId(ConsulServiceIdentifier registryServiceIdentifier, CheckType checkType) {
        return registryServiceIdentifier.getServiceIdentifier()
                .withParts(checkType.getCheckId())
                .toMachineString();
    }

    @Override
    public void registerCheck(RegistryServiceIdentifier serviceIdentifier, CheckType checkType)
            throws ServiceOperationException {
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
                try {
                    consulClient.agentCheckRegister(newCheck, consulConfig.getToken());
                    log.debug("Registered check {}:{}", serviceIdentifier, checkType);
                } catch(OperationException e) {
                    throw new ServiceOperationException(serviceIdentifier, "Failed to register check "+checkType, e);
               }
            });
        }
    }

    @Override
    public void unregisterCheck(RegistryServiceIdentifier serviceIdentifier, CheckType checkType)
            throws ServiceOperationException {
        if (serviceIdentifier instanceof ConsulServiceIdentifier) {
            registryMetrics.unregisterCheck(() -> {
                if(checkExists((ConsulServiceIdentifier) serviceIdentifier, checkType)) {
                    log.debug("Deregistering check {}:{}", serviceIdentifier, checkType);
                    try {
                        consulClient.agentCheckDeregister(
                                createCheckId((ConsulServiceIdentifier) serviceIdentifier, checkType),
                                consulConfig.getToken());
                        log.debug("Deregistered check {}:{}", serviceIdentifier, checkType);
                    } catch(OperationException e) {
                        throw new ServiceOperationException(serviceIdentifier, "Failed to deregister check "+checkType, e);
                    }
                }

            });
        }
    }

    @Override
    public void updateCheck(RegistryServiceIdentifier serviceIdentifier, CheckType checkType, CheckStatus checkStatus,
            String message) throws ServiceOperationException {
        if (serviceIdentifier instanceof ConsulServiceIdentifier) {
            registryMetrics.updateCheck(() -> {
                log.debug("Updating check {}:{} with {}", serviceIdentifier, checkType, checkStatus);

                String checkId = createCheckId((ConsulServiceIdentifier) serviceIdentifier, checkType);

                try {
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
                } catch(OperationException e) {
                    throw new ServiceOperationException(serviceIdentifier, "Failed to update check "+checkType+" to "+checkStatus, e);
                }
            });
        }
    }

    private boolean checkExists(ConsulServiceIdentifier serviceIdentifier, CheckType checkType) {
        String checkId = createCheckId(serviceIdentifier, checkType);
        var agentChecks = consulClient.getAgentChecks().getValue();
        return agentChecks.containsKey(checkId);
    }
}
