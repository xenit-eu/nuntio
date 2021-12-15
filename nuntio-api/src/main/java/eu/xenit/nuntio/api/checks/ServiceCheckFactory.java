package eu.xenit.nuntio.api.checks;

import java.util.Map;

public interface ServiceCheckFactory {
    boolean supportsCheckType(String type);
    ServiceCheck createCheck(String type, String id, Map<String, String> options) throws InvalidCheckException;
}
