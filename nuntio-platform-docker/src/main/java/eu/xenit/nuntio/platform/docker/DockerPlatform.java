package eu.xenit.nuntio.platform.docker;

import eu.xenit.nuntio.api.checks.InvalidCheckException;
import eu.xenit.nuntio.api.identifier.PlatformIdentifier;
import eu.xenit.nuntio.api.platform.PlatformServiceDescription;
import eu.xenit.nuntio.api.platform.PlatformServiceEvent;
import eu.xenit.nuntio.api.platform.PlatformServiceIdentifier;
import eu.xenit.nuntio.api.platform.ServicePlatform;
import eu.xenit.nuntio.api.platform.metrics.PlatformMetrics;
import eu.xenit.nuntio.api.platform.stream.EventStream;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Container;
import eu.xenit.nuntio.platform.docker.config.parser.InvalidMetadataException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@AllArgsConstructor
public class DockerPlatform implements ServicePlatform {

    private final DockerClient dockerClient;
    private final DockerContainerServiceDescriptionFactory serviceDescriptionFactory;
    private final DockerContainerWatcher containerWatcher;
    private final DockerPlatformEventFactory eventFactory;
    private final PlatformMetrics platformMetrics;

    @Override
    public Set<PlatformServiceDescription> findAll() {
        List<Container> containers = dockerClient.listContainersCmd().exec();

        return containers.stream()
                .map(container -> new DockerContainerIdServiceIdentifier(container.getId()))
                .map(this::find)
                .flatMap(Optional::stream)
                .collect(Collectors.toSet());
    }

    @Override
    public Optional<PlatformServiceDescription> find(PlatformServiceIdentifier identifier) {
        if (identifier instanceof DockerContainerIdServiceIdentifier) {
            var containerServiceIdentifier = (DockerContainerIdServiceIdentifier) identifier;
            try {
                var response = dockerClient.inspectContainerCmd(containerServiceIdentifier.getContainerId())
                        .exec();

                return Optional.of(serviceDescriptionFactory.createServiceDescription(response));
            } catch (NotFoundException e) {
                return Optional.empty();
            } catch(InvalidMetadataException e) {
                log.error("Invalid metadata on platform service {}. Ignoring service.", identifier, e);
                return Optional.empty();
            }
        }
        log.error("Received identifier {} of unsupported type.", identifier);
        return Optional.empty();
    }

    @Override
    public Optional<PlatformServiceDescription> find(PlatformIdentifier platformIdentifier) {
        return DockerContainerIdServiceIdentifier.fromPlatformIdentifier(platformIdentifier)
                .flatMap(this::find);
    }

    @Override
    public EventStream<PlatformServiceEvent> eventStream() {
        return containerWatcher.getEventStream()
                .map(eventFactory::createEvent)
                .flatMap(Optional::stream)
                .peek(event -> platformMetrics.event(event.getEventType()));
    }
}
