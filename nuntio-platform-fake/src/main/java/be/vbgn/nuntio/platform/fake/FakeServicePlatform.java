package be.vbgn.nuntio.platform.fake;

import be.vbgn.nuntio.api.SharedIdentifier;
import be.vbgn.nuntio.api.platform.PlatformServiceDescription;
import be.vbgn.nuntio.api.platform.PlatformServiceEvent;
import be.vbgn.nuntio.api.platform.PlatformServiceEvent.EventType;
import be.vbgn.nuntio.api.platform.PlatformServiceHealth;
import be.vbgn.nuntio.api.platform.PlatformServiceIdentifier;
import be.vbgn.nuntio.api.platform.PlatformServiceState;
import be.vbgn.nuntio.api.platform.ServicePlatform;
import be.vbgn.nuntio.api.platform.SimplePlatformServiceDescription;
import be.vbgn.nuntio.api.platform.SimplePlatformServiceDescription.SimplePlatformServiceDescriptionBuilder;
import be.vbgn.nuntio.api.platform.stream.BlockingQueueEventStream;
import be.vbgn.nuntio.api.platform.stream.EventStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Function;
import java.util.function.Predicate;

public class FakeServicePlatform implements ServicePlatform {

    private BlockingQueue<PlatformServiceEvent> serviceEventQueue = new LinkedBlockingQueue<>();
    private Map<FakeServiceIdentifier, SimplePlatformServiceDescription> services = new HashMap<>();

    @Override
    public Set<PlatformServiceDescription> findAll() {
        return new HashSet<>(services.values());
    }

    @Override
    public Optional<PlatformServiceDescription> find(PlatformServiceIdentifier identifier) {
        return Optional.ofNullable(services.get(identifier));
    }

    @Override
    public Optional<PlatformServiceDescription> find(SharedIdentifier sharedIdentifier) {
        return FakeServiceIdentifier.fromSharedIdentifier(sharedIdentifier)
                .flatMap(this::find);
    }

    @Override
    public EventStream<PlatformServiceEvent> eventStream() {
        return new BlockingQueueEventStream<>(serviceEventQueue);
    }

    public void modifyService(FakeServiceIdentifier serviceIdentifier,
            Predicate<SimplePlatformServiceDescription> check,
            Function<SimplePlatformServiceDescriptionBuilder, SimplePlatformServiceDescriptionBuilder> modifier) {
        SimplePlatformServiceDescription serviceDescription = services.get(serviceIdentifier);
        if (!check.test(serviceDescription)) {
            throw new IllegalStateException(
                    "Service " + serviceDescription + " is not in the correct state to execute this operation.");
        }
        services.put(serviceIdentifier, modifier
                .compose(SimplePlatformServiceDescription::toBuilder)
                .andThen(SimplePlatformServiceDescriptionBuilder::build)
                .apply(serviceDescription)
        );
    }

    public void emitEvent(FakeServiceIdentifier serviceIdentifier, EventType eventType) {
        serviceEventQueue.add(new PlatformServiceEvent(eventType, serviceIdentifier));
    }

    private static Predicate<SimplePlatformServiceDescription> hasState(PlatformServiceState state) {
        return (serviceDescription) -> serviceDescription.getState() == state;
    }

    private static Function<SimplePlatformServiceDescriptionBuilder, SimplePlatformServiceDescriptionBuilder> setState(
            PlatformServiceState state) {
        return builder -> builder.state(state);
    }

    public void createService(SimplePlatformServiceDescription service) {
        PlatformServiceIdentifier serviceIdentifier = service.getIdentifier();
        assert serviceIdentifier instanceof FakeServiceIdentifier;
        services.put((FakeServiceIdentifier) serviceIdentifier, service);
    }

    public void startService(FakeServiceIdentifier serviceIdentifier) {
        modifyService(serviceIdentifier, hasState(PlatformServiceState.STOPPED),
                setState(PlatformServiceState.RUNNING));
    }

    public void stopService(FakeServiceIdentifier serviceIdentifier) {
        modifyService(serviceIdentifier,
                hasState(PlatformServiceState.RUNNING).or(hasState(PlatformServiceState.PAUSED)),
                setState(PlatformServiceState.STOPPED));
    }

    public void pauseService(FakeServiceIdentifier serviceIdentifier) {
        modifyService(serviceIdentifier, hasState(PlatformServiceState.RUNNING), setState(PlatformServiceState.PAUSED));
    }

    public void unpauseService(FakeServiceIdentifier serviceIdentifier) {
        modifyService(serviceIdentifier, hasState(PlatformServiceState.PAUSED), setState(PlatformServiceState.RUNNING));
    }

    public void healthcheckService(FakeServiceIdentifier serviceIdentifier, PlatformServiceHealth serviceHealth) {
        modifyService(serviceIdentifier, hasState(PlatformServiceState.RUNNING),
                builder -> builder.health(Optional.of(serviceHealth)));
    }

    public void destroyService(FakeServiceIdentifier serviceIdentifier) {
        services.remove(serviceIdentifier);
    }

    public void clear() {
        services.clear();
        serviceEventQueue.clear();
    }
}
