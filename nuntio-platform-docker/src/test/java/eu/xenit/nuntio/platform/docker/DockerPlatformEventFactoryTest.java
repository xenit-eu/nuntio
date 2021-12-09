package eu.xenit.nuntio.platform.docker;

import static org.junit.jupiter.api.Assertions.assertEquals;

import eu.xenit.nuntio.api.platform.PlatformServiceEvent;
import eu.xenit.nuntio.api.platform.PlatformServiceEvent.EventType;
import com.github.dockerjava.api.model.Event;
import com.github.dockerjava.api.model.EventActor;
import java.util.Collections;
import java.util.Optional;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

class DockerPlatformEventFactoryTest {

    private static <T> T mock(Class<T> clazz) {
        return Mockito.mock(clazz, (invocation) -> {
            throw new UnsupportedOperationException("Not mocked");
        });
    }

    private static Event createEvent(String type) {
        Event event = mock(Event.class);
        EventActor eventActor = mock(EventActor.class);
        Mockito.doReturn(eventActor).when(event).getActor();

        Mockito.doReturn(type).when(event).getAction();
        Mockito.doReturn(Collections.singletonMap("name", "ctr-name")).when(eventActor).getAttributes();
        Mockito.doReturn("abccc").when(eventActor).getId();
        return event;
    }

    @ParameterizedTest
    @CsvSource(value = {
            "start,START",
            "die,STOP",
            "pause,PAUSE",
            "unpause,UNPAUSE",
            "health_status:starting,HEALTHCHECK",
            "health_status:unhealthy,HEALTHCHECK",
            "health_status:healthy,HEALTHCHECK",
            "exec_create,null"
    }, nullValues = "null")
    void createEvent(String dockerEventType, String platformEventType) {

        DockerPlatformEventFactory eventFactory = new DockerPlatformEventFactory();

        var platformEvent = eventFactory.createEvent(createEvent(dockerEventType));

        if (platformEventType != null) {
            assertEquals(Optional.of(EventType.valueOf(platformEventType)),
                    platformEvent.map(PlatformServiceEvent::getEventType));
            assertEquals(Optional.of("abccc"), platformEvent
                    .map(PlatformServiceEvent::getIdentifier)
                    .map(identifier -> (DockerContainerIdServiceIdentifier) identifier)
                    .map(DockerContainerIdServiceIdentifier::getContainerId));
        } else {
            assertEquals(Optional.empty(), platformEvent);
        }


    }

}
