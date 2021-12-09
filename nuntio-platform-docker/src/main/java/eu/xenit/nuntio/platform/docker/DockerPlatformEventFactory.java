package eu.xenit.nuntio.platform.docker;

import eu.xenit.nuntio.api.platform.PlatformServiceEvent;
import eu.xenit.nuntio.api.platform.PlatformServiceEvent.EventType;
import com.github.dockerjava.api.model.Event;
import com.github.dockerjava.api.model.EventActor;
import java.util.Optional;

public class DockerPlatformEventFactory {

    public Optional<PlatformServiceEvent> createEvent(Event event) {
        return createEventType(event.getAction())
                .map(eventType -> new PlatformServiceEvent(eventType, createIdentifier(event.getActor())));
    }


    private Optional<EventType> createEventType(String action) {
        switch (action) {
            case "start":
                return Optional.of(EventType.START);

            case "die":
                return Optional.of(EventType.STOP);

            case "pause":
                return Optional.of(EventType.PAUSE);
            case "unpause":
                return Optional.of(EventType.UNPAUSE);
        }
        if (action.startsWith("health_status:")) {
            return Optional.of(EventType.HEALTHCHECK);
        }
        return Optional.empty();
    }

    private DockerContainerServiceIdentifier createIdentifier(EventActor actor) {
        return new DockerContainerServiceIdentifier(actor.getAttributes().get("name"), actor.getId());
    }

}
