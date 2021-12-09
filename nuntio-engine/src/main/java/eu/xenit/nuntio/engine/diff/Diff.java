package eu.xenit.nuntio.engine.diff;

import java.util.Optional;

public interface Diff {
    default <T extends Diff> Optional<T> cast(Class<T> clazz) {
        if(clazz.isInstance(this)) {
            return Optional.of((T)this);
        } else {
            return Optional.empty();
        }
    }
}
