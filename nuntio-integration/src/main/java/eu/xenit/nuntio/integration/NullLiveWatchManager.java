package eu.xenit.nuntio.integration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;

@Slf4j
public class NullLiveWatchManager implements LiveWatchManager {

    @Override
    public void destroy() {

    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Live watching is not enabled");
    }
}
