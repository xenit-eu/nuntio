package eu.xenit.nuntio.integration;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

public interface LiveWatchManager extends DisposableBean, ApplicationRunner {

    @Override
    void destroy();

    @Override
    void run(ApplicationArguments args);
}
