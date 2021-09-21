package be.vbgn.nuntio.core.service;

import java.util.Collection;
import lombok.Value;

@Value
public class MultipleChecks implements Check {

    Collection<? extends Check> nestedChecks;

    @Override
    public void setFailing(String message) {
        nestedChecks.forEach(check -> check.setFailing(message));
    }

    @Override
    public void setWarning(String message) {

        nestedChecks.forEach(check -> check.setWarning(message));
    }

    @Override
    public void setPassing(String message) {
        nestedChecks.forEach(check -> check.setPassing(message));
    }
}
