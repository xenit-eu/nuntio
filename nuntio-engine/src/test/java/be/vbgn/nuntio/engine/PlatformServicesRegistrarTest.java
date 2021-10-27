package be.vbgn.nuntio.engine;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import be.vbgn.nuntio.api.platform.PlatformServiceConfiguration;
import be.vbgn.nuntio.api.platform.PlatformServiceDescription;
import be.vbgn.nuntio.api.platform.PlatformServiceState;
import be.vbgn.nuntio.api.platform.ServiceBinding;
import be.vbgn.nuntio.api.platform.SimplePlatformServiceDescription;
import be.vbgn.nuntio.api.registry.CheckStatus;
import be.vbgn.nuntio.api.registry.CheckType;
import be.vbgn.nuntio.engine.PlatformServicesRegistrarTest.FakeConfiguration;
import be.vbgn.nuntio.integration.EnableNuntio;
import be.vbgn.nuntio.platform.fake.FakePlatformConfiguration;
import be.vbgn.nuntio.platform.fake.FakeServiceIdentifier;
import be.vbgn.nuntio.platform.fake.FakeServicePlatform;
import be.vbgn.nuntio.registry.fake.FakeCheck;
import be.vbgn.nuntio.registry.fake.FakeRegistryConfiguration;
import be.vbgn.nuntio.registry.fake.FakeServiceRegistry;
import java.util.Collections;
import java.util.Optional;
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
class PlatformServicesRegistrarTest {

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
    PlatformServicesRegistrar platformServicesRegistrar;

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

        platformServicesRegistrar.registerAllServices();

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

        platformServicesRegistrar.registerAllServices();

        assertEquals(2, serviceRegistry.findServices().size(), "2 services registered");

        servicePlatform.stopService(serviceIdentifier);

        platformServicesRegistrar.registerAllServices();

        assertEquals(1, serviceRegistry.findServices().size(), "One service is unregistered");

        servicePlatform.destroyService(serviceIdentifier2);

        platformServicesRegistrar.registerAllServices();

        assertEquals(1, serviceRegistry.findServices().size(), "Other service is not unregistered because it has disappeared");
    }
}
