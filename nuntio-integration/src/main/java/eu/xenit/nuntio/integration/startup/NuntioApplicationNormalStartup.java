package eu.xenit.nuntio.integration.startup;

import eu.xenit.nuntio.engine.PlatformServicesSynchronizer;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

@Slf4j
@AllArgsConstructor
public class NuntioApplicationNormalStartup implements NuntioApplicationStartup {

    private PlatformServicesSynchronizer platformServicesRegistrar;
    private final Retry retry = Retry.of("startup-sync", () -> {
        return RetryConfig.custom()
                .maxAttempts(10)
                .intervalFunction(IntervalFunction.ofExponentialRandomBackoff())
                .retryOnResult(Boolean.FALSE::equals)
                .failAfterMaxAttempts(false)
                .build();
    });

    @Override
    public void run(ApplicationArguments args) {
        while(true) {
            boolean result = retry.executeSupplier(() -> {
                try {
                    log.info("Running existing services registration at application startup");
                    platformServicesRegistrar.syncServices();
                    log.info("Existing services have been registered.");
                    return true;
                } catch (Exception e) {
                    log.error("Failed to register services at startup. Waiting for registration to complete before continuing.", e);
                    return false;
                }
            });
            if(result) {
                return;
            }
        }
    }
}
