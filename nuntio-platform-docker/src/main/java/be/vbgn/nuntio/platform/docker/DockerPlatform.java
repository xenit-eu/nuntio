package be.vbgn.nuntio.platform.docker;

import be.vbgn.nuntio.api.identifier.PlatformIdentifier;
import be.vbgn.nuntio.api.platform.PlatformServiceDescription;
import be.vbgn.nuntio.api.platform.PlatformServiceEvent;
import be.vbgn.nuntio.api.platform.PlatformServiceIdentifier;
import be.vbgn.nuntio.api.platform.ServicePlatform;
import be.vbgn.nuntio.api.platform.stream.EventStream;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.exception.NotFoundException;
import com.github.dockerjava.api.model.Container;
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
                .flatMap(Optional::stream);
    }
}
