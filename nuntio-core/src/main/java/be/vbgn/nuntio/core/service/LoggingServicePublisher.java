package be.vbgn.nuntio.core.service;

import be.vbgn.nuntio.core.service.Service.Identifier;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class LoggingServicePublisher implements ServicePublisher {

    @Override
    public void publish(Service service) {
        log.info("Publish {}", service);
    }

    @Override
    public void unpublish(Identifier serviceIdentifier) {
        log.info("Unpublish {}", serviceIdentifier);

    }

    @Override
    public Check registerCheck(Identifier serviceIdentifier, CheckType type) {
        return new LoggingCheck(serviceIdentifier, type);
    }

    @Override
    public void unregisterCheck(Identifier serviceIdentifier, CheckType checkType) {

    }

    @Value
    private static class LoggingCheck implements Check {

        Identifier serviceIdentifier;
        CheckType checkType;


        @Override
        public void setFailing(String message) {
            log.info("{} check {} failing: {}", serviceIdentifier, checkType, message);
        }

        @Override
        public void setWarning(String message) {
            log.info("{} check {} warning: {}", serviceIdentifier, checkType, message);
        }

        @Override
        public void setPassing(String message) {
            log.debug("{} check {} passing: {}", serviceIdentifier, checkType, message);
        }
    }
}
