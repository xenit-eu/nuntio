package be.vbgn.nuntio.registry.fake;

import be.vbgn.nuntio.api.registry.CheckStatus;
import be.vbgn.nuntio.api.registry.CheckType;
import be.vbgn.nuntio.api.registry.RegistryServiceDescription;
import be.vbgn.nuntio.api.registry.RegistryServiceIdentifier;
import be.vbgn.nuntio.api.registry.ServiceRegistry;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Value;

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
    public RegistryServiceIdentifier registerService(RegistryServiceDescription description) {
        FakeServiceIdentifier serviceIdentifier = FakeServiceIdentifier.create(description.getSharedIdentifier());
        services.put(serviceIdentifier, description);
        return serviceIdentifier;
    }

    @Override
    public void unregisterService(RegistryServiceIdentifier serviceIdentifier) {
        assert serviceIdentifier instanceof FakeServiceIdentifier;
        var checks = getChecksForService((FakeServiceIdentifier) serviceIdentifier);
        checks.forEach(check -> {
            unregisterCheck(serviceIdentifier, check.getType());
        });

        services.remove(serviceIdentifier);
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
        checks.replace(new CheckKey((FakeServiceIdentifier) serviceIdentifier, checkType),
                new CheckValue(checkStatus, message));
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

}
