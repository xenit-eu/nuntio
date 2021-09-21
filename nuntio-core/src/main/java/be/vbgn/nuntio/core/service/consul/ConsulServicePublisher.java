package be.vbgn.nuntio.core.service.consul;

import be.vbgn.nuntio.core.service.Check;
import be.vbgn.nuntio.core.service.CheckType;
import be.vbgn.nuntio.core.service.MultipleChecks;
import be.vbgn.nuntio.core.service.Service;
import be.vbgn.nuntio.core.service.Service.Identifier;
import be.vbgn.nuntio.core.service.ServiceRegistry;
import be.vbgn.nuntio.core.service.consul.ConsulConfig.CheckConfig;
import com.ecwid.consul.v1.ConsulClient;
import com.ecwid.consul.v1.agent.model.NewCheck;
import com.ecwid.consul.v1.agent.model.NewService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class ConsulServicePublisher implements ServiceRegistry {

    private static final String NUNTIO_SID = "nuntio-sid";
    private final ConsulClient consulClient;
    private final ConsulConfig consulConfig;

    @Override
    public void publish(Service service) {
        NewService newService = new NewService();
        newService.setName(service.getName());
        newService.setId(generateServiceId(service));

        newService.setAddress(service.getBinding().getIp());
        newService.setPort(service.getBinding().getPort());
        newService.setTags(new ArrayList<>(service.getTags()));
        Map<String, String> metadata = new HashMap<>(service.getMetadata());
        metadata.put(NUNTIO_SID, service.getServiceIdentifier().toSid());
        newService.setMeta(metadata);

        log.debug("Register {}", newService);
        consulClient.agentServiceRegister(newService, consulConfig.getToken());
    }

    private static String generateServiceId(Service service) {
        return Stream.of(service.getName(), "nuntio", service.getServiceIdentifier().getContainerId(),
                        service.getServiceIdentifier().getContainerName(), service.getOriginalBinding().getPort())
                .filter(Objects::nonNull)
                .map(Objects::toString)
                .collect(Collectors.joining("-"));
    }

    @Override
    public void unpublish(Identifier serviceIdentifier) {
        findServicesWithIdentifier(serviceIdentifier)
                .forEach(service -> {
                    log.debug("Deregister {}", service);
                    consulClient.agentServiceDeregister(service.getId(), consulConfig.getToken());
                });
    }

    public Stream<com.ecwid.consul.v1.agent.model.Service> findServicesWithIdentifier(Identifier identifier) {
        return consulClient.getAgentServices()
                .getValue()
                .values()
                .stream()
                .filter(service -> Objects.equals(service.getMeta().get(NUNTIO_SID), identifier.toSid()));
    }

    @Override
    public Set<Identifier> listServices() {
        return consulClient.getAgentServices()
                .getValue()
                .values()
                .stream()
                .map(service -> Identifier.fromSid(service.getMeta().get(NUNTIO_SID)))
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());
    }

    @Override
    public Check registerCheck(Identifier serviceIdentifier, CheckType type) {
        var agentChecks = consulClient.getAgentChecks().getValue().values();
        var checks = findServicesWithIdentifier(serviceIdentifier)
                .map(service -> {
                    String checkId = service.getId() + ":" + type.getCheckId();

                    var existingCheck = agentChecks.stream()
                            .filter(check -> Objects.equals(check.getCheckId(), checkId)).findFirst();
                    if (existingCheck.isEmpty()) {
                        NewCheck newCheck = new NewCheck();
                        newCheck.setServiceId(service.getId());
                        newCheck.setName(type.getTitle());
                        newCheck.setNotes(type.getDescription());
                        CheckConfig checkConfig = consulConfig.getChecks().get(type);
                        if (checkConfig.getDeregisterCriticalServiceAfter() != null
                                && !checkConfig.getDeregisterCriticalServiceAfter().isEmpty()) {
                            newCheck.setDeregisterCriticalServiceAfter(checkConfig.getDeregisterCriticalServiceAfter());
                        }
                        if (checkConfig.getTtl() != null && !checkConfig.getTtl().isEmpty()) {
                            newCheck.setTtl(checkConfig.getTtl());
                        }
                        newCheck.setId(checkId);

                        log.debug("Register check {}", newCheck);
                        consulClient.agentCheckRegister(newCheck, consulConfig.getToken());
                    }
                    return checkId;
                })
                .map(ConsulCheck::new)
                .collect(Collectors.toSet());

        return new MultipleChecks(checks);
    }

    @Override
    public void unregisterCheck(Identifier serviceIdentifier, CheckType checkType) {
        var agentChecks = consulClient.getAgentChecks().getValue().values();
        findServicesWithIdentifier(serviceIdentifier)
                .forEach(service -> {
                    String checkId = service.getId() + ":" + checkType.getCheckId();
                    var existingCheck = agentChecks.stream()
                            .filter(check -> Objects.equals(check.getCheckId(), checkId)).findFirst();
                    existingCheck.ifPresent(check -> {
                        log.debug("Deregister check {}", check);
                        consulClient.agentCheckDeregister(checkId, consulConfig.getToken());
                    });
                });
    }

    @Value
    private class ConsulCheck implements Check {

        String checkId;


        @Override
        public void setFailing(String message) {
            log.trace("Check {} failing", checkId);
            consulClient.agentCheckFail(checkId, message, consulConfig.getToken());
        }

        @Override
        public void setWarning(String message) {
            log.trace("Check {} warning", checkId);
            consulClient.agentCheckWarn(checkId, message, consulConfig.getToken());
        }

        @Override
        public void setPassing(String message) {
            log.trace("Check {} passing", checkId);
            consulClient.agentCheckPass(checkId, message, consulConfig.getToken());
        }
    }
}
