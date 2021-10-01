package be.vbgn.nuntio.platform.fake;

import be.vbgn.nuntio.api.SharedIdentifier;
import be.vbgn.nuntio.api.platform.PlatformServiceIdentifier;
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

    public static Optional<FakeServiceIdentifier> fromSharedIdentifier(SharedIdentifier sharedIdentifier) {
        if (sharedIdentifier.getContext().equals(CONTEXT)) {
            return Optional.of(new FakeServiceIdentifier(Integer.parseInt(sharedIdentifier.part(0), 16)));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public SharedIdentifier getSharedIdentifier() {
        return SharedIdentifier.of(CONTEXT, Integer.toHexString(serviceId));
    }
}
