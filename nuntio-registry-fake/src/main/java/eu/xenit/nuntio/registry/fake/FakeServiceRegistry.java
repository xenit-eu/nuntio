package eu.xenit.nuntio.registry.fake;

import eu.xenit.nuntio.api.registry.CheckStatus;
import eu.xenit.nuntio.api.registry.CheckType;
import eu.xenit.nuntio.api.registry.RegistryServiceDescription;
import eu.xenit.nuntio.api.registry.RegistryServiceIdentifier;
import eu.xenit.nuntio.api.registry.ServiceRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FakeServiceRegistry implements ServiceRegistry {

    private final Map<FakeServiceIdentifier, RegistryServiceDescription> services = new HashMap<>();
    private final Map<CheckKey, CheckValue> checks = new HashMap<>();


    @Value
    private static class CheckKey {

        FakeServiceIdentifier serviceIdentifier;
        CheckType checkType;
    }

    @Value
    private static class CheckValue {

        CheckStatus status;
        String message;
    }

    @Override
    public Set<? extends RegistryServiceIdentifier> findServices() {
        return services.keySet();
    }

    @Override
    public Optional<RegistryServiceDescription> findServiceDescription(RegistryServiceIdentifier serviceIdentifier) {
        if(serviceIdentifier instanceof FakeServiceIdentifier) {
            return Optional.ofNullable(services.get(serviceIdentifier));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public RegistryServiceIdentifier registerService(RegistryServiceDescription description) {
        FakeServiceIdentifier serviceIdentifier = FakeServiceIdentifier.create(description);
        var replacedService = services.put(serviceIdentifier, description);
        if(replacedService != null) {
            log.info("Register service {} (replaces {})", description, replacedService);
        } else {
            log.info("Registered new service {}", description);
        }
        return serviceIdentifier;
    }

    @Override
    public void unregisterService(RegistryServiceIdentifier serviceIdentifier) {
        assert serviceIdentifier instanceof FakeServiceIdentifier;
        var checks = getChecksForService((FakeServiceIdentifier) serviceIdentifier);
        checks.forEach(check -> {
            unregisterCheck(serviceIdentifier, check.getType());
        });

        var removedService = services.remove(serviceIdentifier);
        if(removedService != null) {
            log.info("Deregister service {}", removedService);
        } else {
            log.warn("Attempt to deregister service {}, but it does not exist", serviceIdentifier);
        }
    }

    @Override
    public void registerCheck(RegistryServiceIdentifier serviceIdentifier, CheckType checkType) {
        assert serviceIdentifier instanceof FakeServiceIdentifier;
        checks.put(new CheckKey((FakeServiceIdentifier) serviceIdentifier, checkType),
                new CheckValue(CheckStatus.FAILING, "Check created"));
    }

    @Override
    public void unregisterCheck(RegistryServiceIdentifier serviceIdentifier, CheckType checkType) {
        assert serviceIdentifier instanceof FakeServiceIdentifier;
        checks.remove(new CheckKey((FakeServiceIdentifier) serviceIdentifier, checkType));
    }

    @Override
    public void updateCheck(RegistryServiceIdentifier serviceIdentifier, CheckType checkType, CheckStatus checkStatus,
            String message) {
        assert serviceIdentifier instanceof FakeServiceIdentifier;
        CheckKey checkKey = new CheckKey((FakeServiceIdentifier) serviceIdentifier, checkType);
        if(!checks.containsKey(checkKey)) {
            throw new IllegalStateException("Updating a check that is not first registered is not possible: "+checkKey);
        }
        log.debug("Update service check {} to status {}", checkKey, checkStatus);
        checks.replace(checkKey, new CheckValue(checkStatus, message));
    }

    public Map<FakeServiceIdentifier, RegistryServiceDescription> getServices() {
        return services;
    }

    public Map<FakeServiceIdentifier, Set<FakeCheck>> getChecks() {
        return checks.entrySet()
                .stream()
                .collect(
                        Collectors.groupingBy(
                                entry -> entry.getKey().getServiceIdentifier(),
                                Collectors.mapping(
                                        entry -> new FakeCheck(
                                                entry.getKey().getCheckType(),
                                                entry.getValue().getStatus(),
                                                entry.getValue().getMessage()
                                        ),
                                        Collectors.toSet()
                                )
                        )
                );

    }

    public Set<FakeCheck> getChecksForService(FakeServiceIdentifier serviceIdentifier) {
        return checks.entrySet()
                .stream()
                .filter(entry -> Objects.equals(entry.getKey().getServiceIdentifier(), serviceIdentifier))
                .map(entry -> new FakeCheck(
                        entry.getKey().getCheckType(),
                        entry.getValue().getStatus(),
                        entry.getValue().getMessage()
                ))
                .collect(Collectors.toSet());
    }

    public void clear() {
        services.clear();
        checks.clear();
    }
}
