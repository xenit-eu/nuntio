package eu.xenit.nuntio.registry.fake.checks;

import eu.xenit.nuntio.api.checks.ServiceCheck;
import eu.xenit.nuntio.api.checks.ServiceCheckFactory;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;

@AllArgsConstructor
@RequiredArgsConstructor
public class FakeCheckFactory implements ServiceCheckFactory {
    private String checkPrefix = "fake:";

    @Override
    public boolean supportsCheckType(String type) {
        return type.startsWith(checkPrefix);
    }

    @Override
    public ServiceCheck createCheck(String type, String id, Map<String, String> options) {
        if(!supportsCheckType(type)) {
            throw new IllegalArgumentException("Check type "+type+" is not supported.");
        }
        return new FakeCheck(id, type, options);
    }
}
