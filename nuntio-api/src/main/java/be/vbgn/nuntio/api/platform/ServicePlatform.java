package be.vbgn.nuntio.api.platform;

import be.vbgn.nuntio.api.SharedIdentifier;
import be.vbgn.nuntio.api.platform.stream.EventStream;
import java.util.Optional;
import java.util.Set;

public interface ServicePlatform {

    Set<PlatformServiceDescription> findAll();

    Optional<PlatformServiceDescription> find(PlatformServiceIdentifier identifier);

    Optional<PlatformServiceDescription> find(SharedIdentifier sharedIdentifier);

    EventStream<PlatformServiceEvent> eventStream();

}
