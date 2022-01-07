package eu.xenit.nuntio.api.registry.errors;

public class RegistryOperationException extends Exception {

    public RegistryOperationException() {
        super();
    }

    public RegistryOperationException(String message) {
        super(message);
    }

    public RegistryOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public RegistryOperationException(Throwable cause) {
        super(cause);
    }
}
