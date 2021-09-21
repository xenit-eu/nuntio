package be.vbgn.nuntio.core.docker;

import be.vbgn.nuntio.core.service.Check;
import be.vbgn.nuntio.core.service.CheckType;
import be.vbgn.nuntio.core.service.MultipleChecks;
import be.vbgn.nuntio.core.service.Service;
import be.vbgn.nuntio.core.service.Service.Identifier;
import be.vbgn.nuntio.core.service.ServiceConfiguration;
import be.vbgn.nuntio.core.service.ServiceConfiguration.ServiceBinding;
import be.vbgn.nuntio.core.service.ServicePublisher;
import be.vbgn.nuntio.core.service.ServiceRegistry;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.HealthState;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectContainerResponse.ContainerState;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Container;
import com.github.dockerjava.api.model.ContainerNetwork;
import com.github.dockerjava.api.model.Event;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.NetworkSettings;
import com.github.dockerjava.api.model.Ports.Binding;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@AllArgsConstructor(onConstructor_ = {@Autowired})
public class DockerContainerHandler {

    private final DockerClient dockerClient;
    private final DockerConfig dockerConfig;
    private final ServiceConfigurationFactory serviceConfigurationFactory;
    private final ServiceFactory serviceFactory;
    private final List<ServicePublisher> publishers;
    private final List<ServiceRegistry> registries;

    public void handleEvent(Event event) {
        switch (event.getAction()) {
            case "start":
                handleEventCreate(event);
                break;

            case "die":
                handleEventDestroy(event);
                break;

            case "pause":
            case "unpause":
            case "health_status":
                handleGenericUpdate(event);
                break;
        }
    }


    private void handleEventCreate(Event event) {
        serviceConfigurationFactory.createIdentifierFromEvent(event).ifPresent(serviceIdentifier -> {
            Set<ServiceConfiguration> discoveredServiceConfigurations = serviceConfigurationFactory.createConfigurationFromEvent(
                    event);
            Set<Service> services = createServices(serviceIdentifier, discoveredServiceConfigurations);
            registerServices(serviceIdentifier, services);
        });
    }

    private void handleEventDestroy(Event event) {
        serviceConfigurationFactory.createIdentifierFromEvent(event).ifPresent(this::unpublish);
    }


    private void handleGenericUpdate(Event event) {
        serviceConfigurationFactory.createIdentifierFromEvent(event)
                .ifPresent(this::updateChecks);
    }


    @Scheduled(fixedRateString = "PT1M", initialDelay = 0)
    public void antiEntropy() {
        log.debug("Running anti-entropy");
        Map<Identifier, Set<ServiceRegistry>> allRegisteredServices = new HashMap<>();

        // Remove services that have been published in any registry but have been removed from docker
        // Also updates healthchecks for services that still exist
        for (ServiceRegistry publisher : registries) {
            Set<Identifier> registeredServices = publisher.listServices();
            for (Identifier registeredService : registeredServices) {
                try {
                    InspectContainerResponse inspectContainerResponse = dockerClient.inspectContainerCmd(
                            registeredService.getContainerId()).exec();
                    if (Boolean.FALSE.equals(inspectContainerResponse.getState().getRunning())) {
                        log.info("Found registered {}, but container is no longer running", registeredService);
                        unpublish(registeredService);
                    } else {
                        updateChecks(registeredService);
                        allRegisteredServices.computeIfAbsent(registeredService, key -> new HashSet<>()).add(publisher);
                    }
                } catch (NotFoundException e) {
                    log.info("Found registered {}, but container does not exist", registeredService, e);
                    unpublish(registeredService);
                }
            }
        }

        // Register services that are not published but exist in docker
        // This can happen if the service registry was down when we tried to register the service
        for (Container container : dockerClient.listContainersCmd().exec()) {
            serviceConfigurationFactory.createIdentifierFromContainer(container)
                    .ifPresent(serviceIdentifier -> {
                        // Find publishers that do not have a service
                        Set<ServiceRegistry> publishersThatKnowService = allRegisteredServices.get(serviceIdentifier);
                        if (publishersThatKnowService == null
                                || publishersThatKnowService.size() != registries.size()) {
                            // If different, some publishers are missing the service. Re-publish to all services
                            Set<ServiceConfiguration> discoveredServiceConfigurations = serviceConfigurationFactory.createConfigurationFromContainer(
                                    container);
                            Set<Service> services = createServices(serviceIdentifier, discoveredServiceConfigurations);
                            if (!services.isEmpty()) {
                                log.warn("Found {} is not registered everywhere. Re-publishing.",
                                        serviceIdentifier);
                                registerServices(serviceIdentifier, services);
                            }
                        }
                    });
        }
    }

    @PreDestroy
    public void unpublishAll() {
        log.info("Application shutting down; unregistering all services");
        for (ServiceRegistry registry : registries) {
            registry.listServices().forEach(this::unpublish);
        }
    }

    private void unpublish(Service.Identifier identifier) {
        publishers.forEach(servicePublisher -> servicePublisher.unpublish(identifier));
    }

    private void publish(Service service) {
        publishers.forEach(servicePublisher -> servicePublisher.publish(service));
    }

    private Check registerCheck(Service.Identifier identifier, CheckType checkType) {
        return new MultipleChecks(publishers
                .stream()
                .map(servicePublisher -> servicePublisher.registerCheck(identifier, checkType))
                .collect(Collectors.toSet()));
    }

    private void unregisterCheck(Identifier serviceIdentifier, CheckType checkType) {
        publishers.forEach(servicePublisher -> servicePublisher.unregisterCheck(serviceIdentifier, checkType));
    }

    private Set<Service> createServices(Identifier serviceIdentifier,
            Set<ServiceConfiguration> discoveredServiceConfigurations) {
        InspectContainerResponse inspectContainerResponse = dockerClient.inspectContainerCmd(
                serviceIdentifier.getContainerId()).exec();
        var portBindings = inspectContainerResponse.getNetworkSettings().getPorts().getBindings();

        Set<ServiceConfiguration> serviceConfigurations = discoveredServiceConfigurations.stream()
                .flatMap(config -> expandAnyBinding(config, portBindings))
                .map(config -> dockerConfig.isInternal() ? replaceWithInternalIp(config, inspectContainerResponse)
                        : replaceWithPublishedPort(config, portBindings))
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());

        if (!discoveredServiceConfigurations.isEmpty() && serviceConfigurations.isEmpty()) {
            log.warn("{} has configurations, but they could not be published. (configurations={})",
                    serviceIdentifier, discoveredServiceConfigurations);
        }

        return serviceConfigurations.stream()
                .flatMap(configuration -> serviceFactory.createFromConfiguration(serviceIdentifier, configuration)
                        .stream())
                .collect(Collectors.toSet());
    }

    private void registerServices(Identifier serviceIdentifier,
            Set<Service> services) {
        services.forEach(this::publish);
        updateChecks(serviceIdentifier);
    }

    private static Optional<ServiceConfiguration> replaceWithPublishedPort(ServiceConfiguration config,
            Map<ExposedPort, Binding[]> portBindings) {
        log.debug("Replacing binding in {} with published ports {}", config, portBindings);
        ServiceBinding serviceBinding = config.getBinding();
        var firstBinding = portBindings
                .entrySet()
                .stream()
                .filter(portBinding -> Objects.equals(portBinding.getKey().getPort(), serviceBinding.getPort()))
                .filter(portBinding -> Objects.equals(portBinding.getKey().getProtocol().toString(),
                        serviceBinding.getProtocol()))
                .map(Entry::getValue)
                .filter(Objects::nonNull)
                .peek(bindings -> {
                    if (bindings.length > 1) {
                        log.warn("{} has multiple published ports {} for binding {}. Selecting one at-random.", config,
                                Arrays.asList(bindings), config.getBinding());
                    }
                })
                .flatMap(Arrays::stream)
                .findFirst();

        if (firstBinding.isEmpty()) {
            log.warn("{} has no published port mapping to {}.", config, config.getBinding());
        }

        return firstBinding
                .map(portBinding -> Integer.parseInt(portBinding.getHostPortSpec()))
                .map(publicPort -> new ServiceBinding(null, publicPort, serviceBinding.getProtocol()))
                .map(config::withBinding);
    }

    private static Stream<ServiceConfiguration> expandAnyBinding(ServiceConfiguration configuration,
            Map<ExposedPort, Binding[]> exposedPorts) {
        if (configuration.getBinding() == ServiceBinding.ANY) {
            log.debug("Replacing {} with all exposed ports {}", configuration, exposedPorts.keySet());
            return exposedPorts.keySet().stream()
                    .map(exposedPort -> new ServiceBinding(null, exposedPort.getPort(),
                            exposedPort.getProtocol().toString()))
                    .map(configuration::withBinding);

        } else {
            return Stream.of(configuration);
        }
    }

    private static Optional<ServiceConfiguration> replaceWithInternalIp(ServiceConfiguration configuration,
            InspectContainerResponse inspectContainerResponse) {
        return Optional.ofNullable(inspectContainerResponse)
                .map(InspectContainerResponse::getNetworkSettings)
                .map(NetworkSettings::getNetworks)
                .map(Map::values)
                .stream()
                .flatMap(Collection::stream)
                .findAny()
                .map(ContainerNetwork::getIpAddress)
                .map(internalIp -> replaceWithInternalIp(configuration, internalIp));
    }

    private static ServiceConfiguration replaceWithInternalIp(ServiceConfiguration configuration, String internalIp) {
        log.debug("Replacing binding in {} with internal IP {}", configuration, internalIp);
        ServiceBinding serviceBinding = configuration.getBinding();
        return configuration.withBinding(
                new ServiceBinding(internalIp, serviceBinding.getPort(), serviceBinding.getProtocol()));
    }


    private void updateChecks(Identifier serviceIdentifier) {

        InspectContainerResponse inspectContainerResponse;
        try {
            inspectContainerResponse = dockerClient.inspectContainerCmd(
                    serviceIdentifier.getContainerId()).exec();
        } catch (NotFoundException e) {
            log.info("Could not update checks on service {}.", serviceIdentifier, e);
            return;
        }

        var healthState = Optional.ofNullable(inspectContainerResponse.getState())
                .map(ContainerState::getHealth);
        String healthStatus = healthState.map(HealthState::getStatus)
                .orElse("none");
        String checkLogs = healthState.map(HealthState::getLog)
                .filter(logs -> !logs.isEmpty())
                .map(logs -> logs.get(logs.size() - 1))
                .map(logEntry -> "exitCode=" + logEntry.getExitCodeLong() + "\n" + logEntry.getOutput())
                .orElse("No logs");
        switch (healthStatus) {
            case "starting":
                registerCheck(serviceIdentifier, CheckType.HEALTHCHECK).setWarning(
                        "Container is starting\n" + checkLogs);
                break;
            case "healthy":
                registerCheck(serviceIdentifier, CheckType.HEALTHCHECK).setPassing(
                        "Container is healthy\n" + checkLogs);
                break;
            case "unhealthy":
                registerCheck(serviceIdentifier, CheckType.HEALTHCHECK).setFailing(
                        "Container is unhealthy\n" + checkLogs);
                break;
            default:
                unregisterCheck(serviceIdentifier, CheckType.HEALTHCHECK);
        }

        if (Boolean.TRUE.equals(inspectContainerResponse.getState().getPaused())) {
            registerCheck(serviceIdentifier, CheckType.PAUSE).setFailing("Container is paused");
        } else {
            unregisterCheck(serviceIdentifier, CheckType.PAUSE);
        }

        registerCheck(serviceIdentifier, CheckType.HEARTBEAT).setPassing("Nuntio knows about this service");
    }

}
