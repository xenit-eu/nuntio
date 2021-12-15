package eu.xenit.nuntio.api.checks;

import lombok.Getter;

public class InvalidCheckException extends Exception {
    @Getter
    private final String checkId;

    public InvalidCheckException(String checkId, String message) {
        super("Check "+checkId+" is invalid: "+message);
        this.checkId = checkId;
    }

    public InvalidCheckException(String checkId, String message, Throwable throwable) {
        this(checkId, message);
        initCause(throwable);
    }
}
