package be.vbgn.nuntio.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.vbgn.nuntio.api.identifier.ServiceIdentifier;
import be.vbgn.nuntio.api.platform.PlatformServiceConfiguration;
import be.vbgn.nuntio.api.platform.PlatformServiceHealth;
import be.vbgn.nuntio.api.platform.PlatformServiceHealth.HealthStatus;
import be.vbgn.nuntio.api.platform.PlatformServiceState;
import be.vbgn.nuntio.api.platform.ServiceBinding;
import be.vbgn.nuntio.api.platform.SimplePlatformServiceDescription;
import be.vbgn.nuntio.api.registry.CheckStatus;
import be.vbgn.nuntio.api.registry.CheckType;
import be.vbgn.nuntio.api.registry.RegistryServiceDescription;
import be.vbgn.nuntio.engine.PlatformServicesRegistrarTest.FakeConfiguration;
import be.vbgn.nuntio.integration.EnableNuntio;
import be.vbgn.nuntio.platform.fake.FakePlatformConfiguration;
import be.vbgn.nuntio.platform.fake.FakeServiceIdentifier;
import be.vbgn.nuntio.platform.fake.FakeServicePlatform;
import be.vbgn.nuntio.registry.fake.FakeCheck;
import be.vbgn.nuntio.registry.fake.FakeRegistryConfiguration;
import be.vbgn.nuntio.registry.fake.FakeServiceRegistry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = FakeConfiguration.class, properties = {
        "nuntio.live.enabled=false",
        "nuntio.anti-entropy.enabled=false"
})
class AntiEntropyDaemonTest {

    @Configuration
    @EnableNuntio
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
    void registersExistingServices() {
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

        antiEntropyDaemon.runAntiEntropy();

        assertEquals(1, serviceRegistry.findServices().size(), "1 service is registered");

        var registeredServiceOptional = serviceRegistry.getServices().values().stream().findFirst();
        assertTrue(registeredServiceOptional.isPresent(), "Service is registered");

        var registeredService = registeredServiceOptional.get();

        assertEquals("proxy-front", registeredService.getName());
        assertEquals("80", registeredService.getPort());
        assertEquals(Optional.empty(), registeredService.getAddress());
        assertEquals(Collections.emptySet(), registeredService.getTags());
    }


    @Test
    void unregistersStoppedServices() {
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

        antiEntropyDaemon.runAntiEntropy();

        assertEquals(1, serviceRegistry.findServices().size(), "One service is unregistered");

        servicePlatform.destroyService(serviceIdentifier2);

        antiEntropyDaemon.runAntiEntropy();

        assertEquals(0, serviceRegistry.findServices().size(), "Other service is unregistered");
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

        antiEntropyDaemon.runAntiEntropy();

        assertEquals(1, serviceRegistry.getServices().size(), "Service is registered");

        var registeredServiceId = serviceRegistry.getServices().keySet().stream().findFirst().get();
        var registeredChecks = serviceRegistry.getChecksForService(registeredServiceId)
                .stream().map(FakeCheck::withoutMessage).collect(Collectors.toSet());

        assertEquals(Set.of(
                FakeCheck.withoutMessage(CheckType.HEARTBEAT, CheckStatus.PASSING),
                FakeCheck.withoutMessage(CheckType.HEALTHCHECK, CheckStatus.WARNING)
        ), registeredChecks);

        antiEntropyDaemon.runAntiEntropy();

        registeredChecks = serviceRegistry.getChecksForService(registeredServiceId)
                .stream().map(FakeCheck::withoutMessage).collect(Collectors.toSet());

        assertEquals(Set.of(
                FakeCheck.withoutMessage(CheckType.HEARTBEAT, CheckStatus.PASSING),
                FakeCheck.withoutMessage(CheckType.HEALTHCHECK, CheckStatus.WARNING)
        ), registeredChecks);

        servicePlatform.healthcheckService(serviceIdentifier, new PlatformServiceHealth(HealthStatus.HEALTHY, "Service is up"));

        antiEntropyDaemon.runAntiEntropy();

        registeredChecks = serviceRegistry.getChecksForService(registeredServiceId)
                .stream().map(FakeCheck::withoutMessage).collect(Collectors.toSet());

        assertEquals(Set.of(
                FakeCheck.withoutMessage(CheckType.HEARTBEAT, CheckStatus.PASSING),
                FakeCheck.withoutMessage(CheckType.HEALTHCHECK, CheckStatus.PASSING)
        ), registeredChecks);

        servicePlatform.healthcheckService(serviceIdentifier, new PlatformServiceHealth(HealthStatus.UNHEALTHY, "Service is in bad health"));

        antiEntropyDaemon.runAntiEntropy();

        registeredChecks = serviceRegistry.getChecksForService(registeredServiceId)
                .stream().map(FakeCheck::withoutMessage).collect(Collectors.toSet());

        assertEquals(Set.of(
                FakeCheck.withoutMessage(CheckType.HEARTBEAT, CheckStatus.PASSING),
                FakeCheck.withoutMessage(CheckType.HEALTHCHECK, CheckStatus.FAILING)
        ), registeredChecks);

        servicePlatform.pauseService(serviceIdentifier);;

        antiEntropyDaemon.runAntiEntropy();

        registeredChecks = serviceRegistry.getChecksForService(registeredServiceId)
                .stream().map(FakeCheck::withoutMessage).collect(Collectors.toSet());

        assertEquals(Set.of(
                FakeCheck.withoutMessage(CheckType.HEARTBEAT, CheckStatus.PASSING),
                FakeCheck.withoutMessage(CheckType.HEALTHCHECK, CheckStatus.FAILING),
                FakeCheck.withoutMessage(CheckType.PAUSE, CheckStatus.FAILING)
        ), registeredChecks);

        servicePlatform.unpauseService(serviceIdentifier);;
        servicePlatform.healthcheckService(serviceIdentifier, new PlatformServiceHealth(HealthStatus.HEALTHY, "Service is up"));

        antiEntropyDaemon.runAntiEntropy();

        registeredChecks = serviceRegistry.getChecksForService(registeredServiceId)
                .stream().map(FakeCheck::withoutMessage).collect(Collectors.toSet());

        assertEquals(Set.of(
                FakeCheck.withoutMessage(CheckType.HEARTBEAT, CheckStatus.PASSING),
                FakeCheck.withoutMessage(CheckType.HEALTHCHECK, CheckStatus.PASSING)
        ), registeredChecks);
    }

    @Test
    void unregistersForNonExistingServices() {
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

        antiEntropyDaemon.runAntiEntropy();

        assertEquals(1, serviceRegistry.getServices().size(), "Service is registered");

        servicePlatform.destroyService(serviceIdentifier);

        antiEntropyDaemon.runAntiEntropy();

        assertEquals(0, serviceRegistry.getServices().size(), "Service is unregistered");
    }

    @Test
    void unregistersForStoppedServices() {
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

        antiEntropyDaemon.runAntiEntropy();

        assertEquals(1, serviceRegistry.getServices().size(), "Service is registered");

        servicePlatform.stopService(serviceIdentifier);

        antiEntropyDaemon.runAntiEntropy();

        assertEquals(0, serviceRegistry.getServices().size(), "Service is unregistered");
    }

    @Test
    void updatesChangeInPublishedPort() {
        var serviceIdentifier = FakeServiceIdentifier.create();
        var serviceConfiguration = PlatformServiceConfiguration.builder()
                .serviceBinding(ServiceBinding.fromPort(80))
                .serviceName("proxy-front")
                .build();
        var service = SimplePlatformServiceDescription.builder()
                .identifier(serviceIdentifier)
                .state(PlatformServiceState.RUNNING)
                .serviceConfiguration(serviceConfiguration)
                .build();
        servicePlatform.createService(service);

        antiEntropyDaemon.runAntiEntropy();

        assertEquals(Arrays.asList(
                RegistryServiceDescription.builder()
                        .platformIdentifier(serviceIdentifier.getPlatformIdentifier())
                        .serviceIdentifier(ServiceIdentifier.of(serviceIdentifier.getPlatformIdentifier(), serviceConfiguration.getServiceBinding()))
                        .name("proxy-front")
                        .port("80")
                        .build()

        ), new ArrayList<>(serviceRegistry.getServices().values()));

        var serviceConfigurationWithOtherPort = PlatformServiceConfiguration.builder()
                .serviceBinding(ServiceBinding.fromPort(81))
                .serviceName("proxy-front")
                .build();
        var serviceWithOtherPort = SimplePlatformServiceDescription.builder()
                .identifier(serviceIdentifier)
                .state(PlatformServiceState.RUNNING)
                .serviceConfiguration(serviceConfigurationWithOtherPort)
                .build();
        servicePlatform.createService(serviceWithOtherPort);

        antiEntropyDaemon.runAntiEntropy();

        assertEquals(Arrays.asList(
                RegistryServiceDescription.builder()
                        .platformIdentifier(serviceIdentifier.getPlatformIdentifier())
                        .serviceIdentifier(ServiceIdentifier.of(serviceIdentifier.getPlatformIdentifier(), serviceConfigurationWithOtherPort.getServiceBinding()))
                        .name("proxy-front")
                        .port("81")
                        .build()

        ), new ArrayList<>(serviceRegistry.getServices().values()));

    }
}
