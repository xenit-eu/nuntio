package eu.xenit.nuntio.platform.docker.config.parser;

public class InvalidMetadataException extends RuntimeException {
    public InvalidMetadataException(String message) {
        super(message);
    }

    public InvalidMetadataException(String message, Throwable e) {
        super(message, e);
    }

    public InvalidMetadataException(Throwable e) {
        super(e);
    }

}
