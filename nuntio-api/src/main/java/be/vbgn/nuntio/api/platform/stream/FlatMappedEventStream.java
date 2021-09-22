package be.vbgn.nuntio.api.platform.stream;

import java.util.Iterator;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.BaseStream;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class FlatMappedEventStream<T, U> implements EventStream<U> {

    private final EventStream<? extends T> originalStream;
    private final Function<? super T, ? extends Stream<? extends U>> flatMapper;
    private Iterator<? extends U> tempStream = null;

    FlatMappedEventStream(EventStream<? extends T> originalStream,
            Function<? super T, ? extends Stream<? extends U>> flatMapper) {
        this.originalStream = originalStream;
        this.flatMapper = flatMapper;
    }

    private Optional<Iterator<? extends U>> nextIterator() {
        do {
            log.trace("nextIterator({}): tempStream={}", this, tempStream);
            if (tempStream != null) {
                log.trace("nextIterator({}): Existing stream: hasNext={}", this, tempStream.hasNext());
                if (tempStream.hasNext()) {
                    return Optional.of(tempStream);
                }
            }
            Optional<? extends T> nextOriginal = originalStream.next();
            if (nextOriginal.isEmpty()) {
                log.trace("nextIterator({}): Original is empty", this);
                return Optional.empty();
            }
            tempStream = nextOriginal
                    .map(flatMapper)
                    .map(BaseStream::iterator)
                    .filter(Iterator::hasNext)
                    .orElse(null);
            log.trace("nextIterator({}): New stream: tempStream={}", this, tempStream);
        } while (tempStream == null);
        return Optional.of(tempStream);
    }

    private Iterator<? extends U> nextIteratorBlocking() {
        log.trace("nextIteratorBlocking({}): tempStream={}", this, tempStream);
        if (tempStream != null) {
            log.trace("nextIteratorBlocking({}): Existing stream: hasNext={}", this, tempStream.hasNext());
            if (tempStream.hasNext()) {
                return tempStream;
            }
        }
        do {
            tempStream = flatMapper.andThen(BaseStream::iterator).apply(originalStream.nextBlocking());
            log.trace("nextIteratorBlocking({}): New stream: tempStream={}; hasNext={}", this, tempStream,
                    tempStream.hasNext());
        } while (!tempStream.hasNext());
        return tempStream;
    }

    @Override
    public Optional<? extends U> next() {
        return nextIterator()
                .filter(Iterator::hasNext)
                .map(Iterator::next);
    }

    @Override
    public U nextBlocking() {
        return nextIteratorBlocking().next();
    }
}
