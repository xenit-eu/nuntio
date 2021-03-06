package eu.xenit.nuntio.engine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import eu.xenit.nuntio.api.platform.PlatformServiceConfiguration;
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
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@SpringBootTest(classes = FakeConfiguration.class, properties = {
        "nuntio.engine.live.enabled=false",
        "nuntio.engine.anti-entropy.enabled=false"
})
class PlatformServicesRegistrarTest {

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
    PlatformServicesSynchronizer platformServicesRegistrar;

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

        platformServicesRegistrar.syncServices();

        assertEquals(1, serviceRegistry.findServices().size(), "1 service is registered");

        var registeredServiceOptional = serviceRegistry.getServices().values().stream().findFirst();
        assertTrue(registeredServiceOptional.isPresent(), "Service is registered");

        var registeredService = registeredServiceOptional.get();

        assertEquals("proxy-front", registeredService.getName());
        assertEquals("80", registeredService.getPort());
        assertEquals(Optional.empty(), registeredService.getAddress());
        assertEquals(Collections.emptySet(), registeredService.getTags());

        var registeredServiceId = serviceRegistry.getServices().keySet().stream().findFirst().get();
        var registeredChecks = serviceRegistry.getChecksForService(registeredServiceId)
                .stream().map(FakeCheck::withoutMessage).collect(Collectors.toSet());

        assertEquals(Collections.singleton(FakeCheck.withoutMessage(CheckType.HEARTBEAT, CheckStatus.PASSING)), registeredChecks);

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

        platformServicesRegistrar.syncServices();

        assertEquals(2, serviceRegistry.findServices().size(), "2 services registered");

        servicePlatform.stopService(serviceIdentifier);

        platformServicesRegistrar.syncServices();

        assertEquals(1, serviceRegistry.findServices().size(), "One service is unregistered");

        servicePlatform.destroyService(serviceIdentifier2);

        platformServicesRegistrar.syncServices();

        assertEquals(0, serviceRegistry.findServices().size(), "Other service is unregistered because it has disappeared");
    }
}
