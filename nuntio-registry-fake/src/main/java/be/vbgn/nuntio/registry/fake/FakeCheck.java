package be.vbgn.nuntio.registry.fake;

import be.vbgn.nuntio.api.registry.CheckStatus;
import be.vbgn.nuntio.api.registry.CheckType;
import lombok.Value;

@Value
public class FakeCheck {

    CheckType type;
    CheckStatus status;
    String message;

    public FakeCheckWithoutMessage withoutMessage() {
        return withoutMessage(type, status);
    }

    public static FakeCheckWithoutMessage withoutMessage(CheckType type, CheckStatus status) {
        return new FakeCheckWithoutMessage(type, status);
    }

    @Value
    public static class FakeCheckWithoutMessage {
        CheckType type;
        CheckStatus status;
    }
}
