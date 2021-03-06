package eu.xenit.nuntio.engine;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.xenit.nuntio.api.platform.PlatformServiceConfiguration;
import eu.xenit.nuntio.api.platform.PlatformServiceEvent.EventType;
import eu.xenit.nuntio.api.platform.PlatformServiceHealth;
import eu.xenit.nuntio.api.platform.PlatformServiceHealth.HealthStatus;
import eu.xenit.nuntio.api.platform.PlatformServiceState;
import eu.xenit.nuntio.api.platform.ServiceBinding;
import eu.xenit.nuntio.api.platform.SimplePlatformServiceDescription;
import eu.xenit.nuntio.api.registry.CheckStatus;
import eu.xenit.nuntio.api.registry.CheckType;
import eu.xenit.nuntio.engine.PlatformServicesRegistrarTest.FakeConfiguration;
import eu.xenit.nuntio.integration.EnableNuntio;
import eu.xenit.nuntio.platform.fake.FakePlatformConfiguration;
import eu.xenit.nuntio.platform.fake.FakeServiceIdentifier;
import eu.xenit.nuntio.platform.fake.FakeServicePlatform;
import eu.xenit.nuntio.registry.fake.FakeCheck;
import eu.xenit.nuntio.registry.fake.FakeRegistryConfiguration;
import eu.xenit.nuntio.registry.fake.FakeServiceRegistry;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = FakeConfiguration.class, properties = {
        "nuntio.engine.live.enabled=true",
        "nuntio.engine.anti-entropy.enabled=false"
})
class LiveWatchDaemonTest {

    @Configuration
    @EnableNuntio
    @EnableAutoConfiguration
    @Import({
            FakePlatformConfiguration.class,
            FakeRegistryConfiguration.class
    })
    public static class FakeConfiguration {

    }

    @Autowired
    FakeServicePlatform servicePlatform;

    @Autowired
    FakeServiceRegistry serviceRegistry;

    @Autowired
    AntiEntropyDaemon antiEntropyDaemon;

    @BeforeEach
    void resetServices() {
        servicePlatform.clear();
        serviceRegistry.clear();
    }

    @Test
    void registersStartingService() {
        var serviceIdentifier = FakeServiceIdentifier.create();
        var service = SimplePlatformServiceDescription.builder()
                .identifier(serviceIdentifier)
                .state(PlatformServiceState.RUNNING)
                .serviceConfiguration(PlatformServiceConfiguration.builder()
                        .serviceBinding(ServiceBinding.fromPort(80))
                        .serviceName("proxy-front")
                        .build())
                .build();
        servicePlatform.createService(service);
        servicePlatform.emitEvent(serviceIdentifier, EventType.START);

        await().until(() -> serviceRegistry.findServices().size(), Matchers.equalTo(1));

        var registeredServiceOptional = serviceRegistry.getServices().values().stream().findFirst();
        assertTrue(registeredServiceOptional.isPresent(), "Service is registered");

        var registeredService = registeredServiceOptional.get();

        assertEquals("proxy-front", registeredService.getName());
        assertEquals("80", registeredService.getPort());
        assertEquals(Optional.empty(), registeredService.getAddress());
        assertEquals(Collections.emptySet(), registeredService.getTags());
    }


    @Test
    void unregistersStoppingService() {
        var serviceIdentifier = FakeServiceIdentifier.create();
        var service = SimplePlatformServiceDescription.builder()
                .identifier(serviceIdentifier)
                .state(PlatformServiceState.RUNNING)
                .serviceConfiguration(PlatformServiceConfiguration.builder()
                        .serviceBinding(ServiceBinding.fromPort(80))
                        .serviceName("proxy-front")
                        .build())
                .build();
        servicePlatform.createService(service);

        var serviceIdentifier2 = FakeServiceIdentifier.create();
        var service2 = SimplePlatformServiceDescription.builder()
                .identifier(serviceIdentifier2)
                .state(PlatformServiceState.RUNNING)
                .serviceConfiguration(PlatformServiceConfiguration.builder()
                        .serviceBinding(ServiceBinding.fromPort(80))
                        .serviceName("proxy-front")
                        .build())
                .build();
        servicePlatform.createService(service2);

        antiEntropyDaemon.runAntiEntropy();

        assertEquals(2, serviceRegistry.findServices().size(), "2 services registered");

        servicePlatform.stopService(serviceIdentifier);
        servicePlatform.emitEvent(serviceIdentifier, EventType.STOP);

        await().until(() -> serviceRegistry.findServices().size(), Matchers.equalTo(1));

        servicePlatform.destroyService(serviceIdentifier2);
        servicePlatform.emitEvent(serviceIdentifier2, EventType.STOP);

        await().until(() -> serviceRegistry.findServices().size(), Matchers.equalTo(0));
    }

    @Test
    void updatesChecksForRunningServices() {
        var serviceIdentifier = FakeServiceIdentifier.create();
        var service = SimplePlatformServiceDescription.builder()
                .identifier(serviceIdentifier)
                .state(PlatformServiceState.RUNNING)
                .health(Optional.of(new PlatformServiceHealth(HealthStatus.STARTING, "Service is starting")))
                .serviceConfiguration(PlatformServiceConfiguration.builder()
                        .serviceBinding(ServiceBinding.fromPort(80))
                        .serviceName("proxy-front")
                        .build())
                .build();
        servicePlatform.createService(service);
        servicePlatform.emitEvent(serviceIdentifier, EventType.START);

        await().until(() -> serviceRegistry.findServices().size(), Matchers.equalTo(1));

        var registeredServiceId = serviceRegistry.getServices().keySet().stream().findFirst().get();

        await().until(() -> serviceRegistry.getChecksForService(registeredServiceId)
                .stream().map(FakeCheck::withoutMessage).collect(Collectors.toSet()), Matchers.equalTo(Set.of(
                FakeCheck.withoutMessage(CheckType.HEARTBEAT, CheckStatus.PASSING),
                FakeCheck.withoutMessage(CheckType.HEALTHCHECK, CheckStatus.WARNING)
        )));

        servicePlatform.healthcheckService(serviceIdentifier, new PlatformServiceHealth(HealthStatus.HEALTHY, "Service is up"));
        servicePlatform.emitEvent(serviceIdentifier, EventType.HEALTHCHECK);

        await().until(() -> serviceRegistry.getChecksForService(registeredServiceId)
                .stream().map(FakeCheck::withoutMessage).collect(Collectors.toSet()), Matchers.equalTo(Set.of(
                FakeCheck.withoutMessage(CheckType.HEARTBEAT, CheckStatus.PASSING),
                FakeCheck.withoutMessage(CheckType.HEALTHCHECK, CheckStatus.PASSING)
        )));

        servicePlatform.healthcheckService(serviceIdentifier, new PlatformServiceHealth(HealthStatus.UNHEALTHY, "Service is in bad health"));
        servicePlatform.emitEvent(serviceIdentifier, EventType.HEALTHCHECK);

        await().until(() -> serviceRegistry.getChecksForService(registeredServiceId)
                .stream().map(FakeCheck::withoutMessage).collect(Collectors.toSet()), Matchers.equalTo(Set.of(
                FakeCheck.withoutMessage(CheckType.HEARTBEAT, CheckStatus.PASSING),
                FakeCheck.withoutMessage(CheckType.HEALTHCHECK, CheckStatus.FAILING)
        )));

        servicePlatform.pauseService(serviceIdentifier);;
        servicePlatform.emitEvent(serviceIdentifier, EventType.PAUSE);

        await().until(() -> serviceRegistry.getChecksForService(registeredServiceId)
                .stream().map(FakeCheck::withoutMessage).collect(Collectors.toSet()), Matchers.equalTo(Set.of(
                FakeCheck.withoutMessage(CheckType.HEARTBEAT, CheckStatus.PASSING),
                FakeCheck.withoutMessage(CheckType.HEALTHCHECK, CheckStatus.FAILING),
                FakeCheck.withoutMessage(CheckType.PAUSE, CheckStatus.FAILING)
        )));

        servicePlatform.unpauseService(serviceIdentifier);;
        servicePlatform.emitEvent(serviceIdentifier, EventType.UNPAUSE);
        servicePlatform.healthcheckService(serviceIdentifier, new PlatformServiceHealth(HealthStatus.HEALTHY, "Service is up"));
        servicePlatform.emitEvent(serviceIdentifier, EventType.HEALTHCHECK);

        await().until(() -> serviceRegistry.getChecksForService(registeredServiceId)
                .stream().map(FakeCheck::withoutMessage).collect(Collectors.toSet()), Matchers.equalTo(Set.of(
                FakeCheck.withoutMessage(CheckType.HEARTBEAT, CheckStatus.PASSING),
                FakeCheck.withoutMessage(CheckType.HEALTHCHECK, CheckStatus.PASSING)
        )));
    }
}
