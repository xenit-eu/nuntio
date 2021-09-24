package be.vbgn.nuntio.app.startup;

import be.vbgn.nuntio.engine.AntiEntropyDaemon;
import be.vbgn.nuntio.engine.LiveWatchDaemon;
import be.vbgn.nuntio.engine.PlatformServicesRegistrar;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor(onConstructor_ = @Autowired)
@Slf4j
@ConditionalOnMissingBean(NuntioApplicationStartup.class)
public class NuntioApplicationNormalStartup implements ApplicationRunner {

    private PlatformServicesRegistrar platformServicesRegistrar;
    private LiveWatchDaemon liveWatchDaemon;
    private ApplicationContext applicationContext;

    @Override
    public void run(ApplicationArguments args) {
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

        Thread liveWatchThread = new Thread(liveWatchDaemon);
        liveWatchThread.setName("LiveWatchDaemon");
        liveWatchThread.setDaemon(true);
        liveWatchThread.start();
    }

}
