package be.vbgn.nuntio.integration.startup;

import be.vbgn.nuntio.engine.AntiEntropyDaemon;
import be.vbgn.nuntio.engine.LiveWatchDaemon;
import be.vbgn.nuntio.engine.PlatformServicesSynchronizer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.Nullable;

@Slf4j
public class NuntioApplicationNormalStartup implements ApplicationRunner, DisposableBean {

    private PlatformServicesSynchronizer platformServicesRegistrar;
    @Nullable
    private LiveWatchDaemon liveWatchDaemon;
    @Nullable
    private Thread liveWatchThread = null;
    private ApplicationContext applicationContext;

    public NuntioApplicationNormalStartup(PlatformServicesSynchronizer platformServicesRegistrar, @Nullable LiveWatchDaemon liveWatchDaemon, ApplicationContext applicationContext) {
        this.platformServicesRegistrar = platformServicesRegistrar;
        this.liveWatchDaemon = liveWatchDaemon;
        this.applicationContext = applicationContext;
    }


    @Override
    public void run(ApplicationArguments args) {
        if (liveWatchDaemon != null) {
            log.info("Starting live watch daemon thread");
            liveWatchThread = new Thread(liveWatchDaemon);
            liveWatchThread.setName("LiveWatchDaemon");
            liveWatchThread.setDaemon(true);
            liveWatchThread.start();
        } else {
            log.info("Live watching is not enabled");
        }

        log.info("Running existing services registration at application startup");
        try {
            platformServicesRegistrar.syncServices();
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

    public void shutdownLiveWatch() {
        if(liveWatchThread != null) {
            log.info("Interrupting live-watch");
            liveWatchThread.interrupt();
            liveWatchDaemon = null;
        }
    }

    @Override
    public void destroy() throws Exception {
        shutdownLiveWatch();
    }
}
