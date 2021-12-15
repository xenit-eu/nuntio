package eu.xenit.nuntio.registry.fake.checks;

import eu.xenit.nuntio.api.checks.ServiceCheck;
import java.util.Map;
import lombok.Value;

@Value
public class FakeCheck implements ServiceCheck {
    String id;
    String type;
    Map<String, String> options;
}
