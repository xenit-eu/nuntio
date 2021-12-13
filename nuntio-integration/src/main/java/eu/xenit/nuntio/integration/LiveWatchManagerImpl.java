package eu.xenit.nuntio.integration;

import eu.xenit.nuntio.engine.LiveWatchDaemon;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.annotation.Order;
import org.springframework.lang.Nullable;

@Slf4j
@Order(1)
public class LiveWatchManagerImpl implements LiveWatchManager {
    private final LiveWatchDaemon liveWatchDaemon;
    @Nullable
    private Thread liveWatchThread = null;

    public LiveWatchManagerImpl(LiveWatchDaemon liveWatchDaemon) {
        this.liveWatchDaemon = liveWatchDaemon;
    }

    @Override
    public void destroy() {
        if(liveWatchThread != null) {
            log.info("Interrupting live-watch");
            liveWatchThread.interrupt();
            liveWatchThread = null;
        }
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting live watch daemon thread");
        liveWatchThread = new Thread(liveWatchDaemon);
        liveWatchThread.setName("LiveWatchDaemon");
        liveWatchThread.start();
    }
}
