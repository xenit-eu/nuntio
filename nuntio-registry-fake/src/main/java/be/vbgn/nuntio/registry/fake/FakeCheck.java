package be.vbgn.nuntio.registry.fake;

import be.vbgn.nuntio.api.registry.CheckStatus;
import be.vbgn.nuntio.api.registry.CheckType;
import lombok.Value;

@Value
public class FakeCheck {

    CheckType type;
    CheckStatus status;
    String message;
}
