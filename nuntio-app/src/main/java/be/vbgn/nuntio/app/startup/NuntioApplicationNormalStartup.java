package be.vbgn.nuntio.app.startup;

import be.vbgn.nuntio.engine.AntiEntropyDaemon;
import be.vbgn.nuntio.engine.LiveWatchDaemon;
import be.vbgn.nuntio.engine.PlatformServicesRegistrar;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnMissingBean(NuntioApplicationStartup.class)
public class NuntioApplicationNormalStartup implements ApplicationRunner {

    private PlatformServicesRegistrar platformServicesRegistrar;
    private LiveWatchDaemon liveWatchDaemon;
    private ApplicationContext applicationContext;

    @Autowired
    public NuntioApplicationNormalStartup(
            PlatformServicesRegistrar platformServicesRegistrar,
            @Autowired(required = false) LiveWatchDaemon liveWatchDaemon,
            ApplicationContext applicationContext
    ) {
        this.platformServicesRegistrar = platformServicesRegistrar;
        this.liveWatchDaemon = liveWatchDaemon;
        this.applicationContext = applicationContext;
    }


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
