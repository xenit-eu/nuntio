package be.vbgn.nuntio.integration.availability;

import be.vbgn.nuntio.engine.availability.AvailabilityManager;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationEventPublisher;

@Slf4j
@AllArgsConstructor
public class AvailabilityManagerImpl implements AvailabilityManager {
    private ApplicationEventPublisher eventPublisher;
    private final Set<Object> failingComponents = Collections.synchronizedSet(new HashSet<>());

    @Override
    public void registerFailure(Object component) {
        if(failingComponents.add(component)) {
            log.warn("Component {} reports new failure. Marking application as not ready.", component);
            AvailabilityChangeEvent.publish(eventPublisher, component, ReadinessState.REFUSING_TRAFFIC);
        }
    }

    @Override
    public void registerSuccess(Object component) {
        if(failingComponents.remove(component)) {
            log.info("Component {} reports success after previous failure", component);

            if(failingComponents.isEmpty()) {
                log.info("No components are failing anymore. Marking application as ready.");
                AvailabilityChangeEvent.publish(eventPublisher, component, ReadinessState.ACCEPTING_TRAFFIC);
            } else {
                log.info("Components {} are still failing.", failingComponents);
            }
        }

    }
}
