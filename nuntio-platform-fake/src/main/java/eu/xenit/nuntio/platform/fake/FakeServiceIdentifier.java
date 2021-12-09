package eu.xenit.nuntio.platform.fake;

import eu.xenit.nuntio.api.identifier.PlatformIdentifier;
import eu.xenit.nuntio.api.platform.PlatformServiceIdentifier;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class FakeServiceIdentifier implements PlatformServiceIdentifier {

    private static final String CONTEXT = FakeServiceIdentifier.class.getName();
    private static final AtomicInteger SERVICE_ID_GENERATOR = new AtomicInteger();

    int serviceId;

    public static FakeServiceIdentifier create() {
        return new FakeServiceIdentifier(SERVICE_ID_GENERATOR.incrementAndGet());
    }

    public static Optional<FakeServiceIdentifier> fromPlatformIdentifier(PlatformIdentifier platformIdentifier) {
        if (platformIdentifier.getContext().equals(CONTEXT)) {
            return Optional.of(new FakeServiceIdentifier(Integer.parseInt(platformIdentifier.part(0), 16)));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public PlatformIdentifier getPlatformIdentifier() {
        return PlatformIdentifier.of(CONTEXT, Integer.toHexString(serviceId));
    }
}
