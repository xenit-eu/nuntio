package be.vbgn.nuntio.core;

import be.vbgn.nuntio.core.service.CheckType;
import be.vbgn.nuntio.core.service.Service;
import be.vbgn.nuntio.core.service.Service.Identifier;
import be.vbgn.nuntio.core.service.ServiceConfiguration.ServiceBinding;
import be.vbgn.nuntio.core.service.ServicePublisher;
import java.util.Collections;
import java.util.List;
import javax.annotation.PreDestroy;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.availability.ApplicationAvailability;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty("nuntio.register-self")
@AllArgsConstructor
public class AutoregisterApplicationStartup {

    private final List<ServicePublisher> publishers;
    private final ApplicationAvailability applicationAvailability;

    private static final Service.Identifier serviceIdentifier = new Identifier("nuntio", null);

    @EventListener
    public void onAvailabilityChange(AvailabilityChangeEvent<ReadinessState> availabilityChangeEvent) {
        switch (availabilityChangeEvent.getState()) {
            case REFUSING_TRAFFIC:
                publishers.forEach(servicePublisher -> {
                    servicePublisher.registerCheck(serviceIdentifier, CheckType.HEARTBEAT)
                            .setFailing("");
                });
                break;
            case ACCEPTING_TRAFFIC:
                publishers.forEach(servicePublisher -> {
                    servicePublisher.registerCheck(serviceIdentifier, CheckType.HEARTBEAT)
                            .setPassing("");
                });
                break;
        }
    }

    @EventListener
    public void onApplicationStarted(ApplicationStartedEvent startingEvent) {
        publishers.forEach(servicePublisher -> {
            servicePublisher.publish(
                    new Service(
                            serviceIdentifier,
                            "nuntio",
                            ServiceBinding.ANY,
                            ServiceBinding.ANY,
                            Collections.emptySet(),
                            Collections.emptyMap()
                    )
            );
            servicePublisher.registerCheck(serviceIdentifier, CheckType.HEARTBEAT)
                    .setWarning("Application is starting up");
        });
    }

    @EventListener
    public void onApplicationFailure(ApplicationFailedEvent failedEvent) {
        publishers.forEach(servicePublisher -> {
            servicePublisher.registerCheck(serviceIdentifier, CheckType.HEARTBEAT)
                    .setFailing(failedEvent.getException().toString());
        });
    }

    @Scheduled(fixedRateString = "PT1M")
    public void healthStatus() {
        publishers.forEach(servicePublisher -> {
            servicePublisher.registerCheck(serviceIdentifier, CheckType.HEARTBEAT).setPassing("");
        });
    }

    @PreDestroy
    public void onShutdown() {
        publishers.forEach(servicePublisher -> {
            servicePublisher.unpublish(serviceIdentifier);
        });
    }

}
