package be.vbgn.nuntio.integration.startup;

import be.vbgn.nuntio.engine.AntiEntropyDaemon;
import be.vbgn.nuntio.engine.LiveWatchDaemon;
import be.vbgn.nuntio.engine.PlatformServicesRegistrar;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;

@Slf4j
@AllArgsConstructor
public class NuntioApplicationNormalStartup implements ApplicationRunner {

    private PlatformServicesRegistrar platformServicesRegistrar;
    @Nullable
    private LiveWatchDaemon liveWatchDaemon;
    private ApplicationContext applicationContext;


    @Override
    public void run(ApplicationArguments args) {
        if (liveWatchDaemon != null) {
            log.info("Starting live watch daemon thread");
            Thread liveWatchThread = new Thread(liveWatchDaemon);
            liveWatchThread.setName("LiveWatchDaemon");
            liveWatchThread.setDaemon(true);
            liveWatchThread.start();
        } else {
            log.info("Live watching is not enabled");
        }

        log.info("Running existing services registration at application startup");
        try {
            platformServicesRegistrar.registerPlatformServices();
        } catch (Exception e) {
            if (applicationContext.getBeanNamesForType(AntiEntropyDaemon.class, true, false).length > 0) {
                log.error(
                        "Failed to register services at startup. Continuing as registration will be retried during anti-entropy scanning.",
                        e);
            } else {
                throw e;
            }
        }

    }

}
