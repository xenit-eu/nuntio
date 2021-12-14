package eu.xenit.nuntio.api.platform.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventStreamTest {

    private FakeEventStream<Object> eventStream;

    @BeforeEach
    void setup() {
        eventStream = new FakeEventStream<>();
    }

    @Test
    void next() {
        assertEquals(Optional.empty(), eventStream.next());

        Object obj = new Object();
        eventStream.appendItem(obj);

        assertEquals(Optional.of(obj), eventStream.next());
        assertEquals(Optional.empty(), eventStream.next());
    }

    @Test
    void nextBlocking() {
        assertThrows(NoSuchElementException.class, () -> {
            eventStream.nextBlocking();
        });

        Object obj = new Object();
        eventStream.appendItem(obj);

        assertEquals(obj, eventStream.nextBlocking());

        assertThrows(NoSuchElementException.class, () -> {
            eventStream.nextBlocking();
        });
    }

    @Test
    void forEach() {
        List<Object> seenItems = new ArrayList<>();

        eventStream.forEach(seenItems::add);

        assertEquals(List.of(), seenItems);

        Object obj = new Object();
        eventStream.appendItem(obj);

        eventStream.forEach(seenItems::add);
        assertEquals(List.of(obj), seenItems);
    }

    @Test
    void forEachBlocking() {
        List<Object> seenItems = new ArrayList<>();

        assertThrows(NoSuchElementException.class, () -> {
            eventStream.forEachBlocking(seenItems::add);
        });

        assertEquals(List.of(), seenItems);

        Object obj = new Object();
        eventStream.appendItem(obj);

        assertThrows(NoSuchElementException.class, () -> {
            eventStream.forEachBlocking(seenItems::add);
        });

        assertEquals(List.of(obj), seenItems);
    }

    @Test
    void map() {
        EventStream<Integer> intStream = eventStream
                .map(o -> Integer.parseInt(o.toString()));

        assertEquals(Optional.empty(), intStream.next());

        eventStream.appendItem("123");

        assertEquals(Optional.of(123), intStream.next());

        eventStream.appendItem("456");

        assertEquals(456, intStream.nextBlocking());

        assertEquals(Optional.empty(), intStream.next());
    }

    @Test
    void flatMap() {
        EventStream<Character> charStream = eventStream
                .map(Object::toString)
                .flatMap(str -> str.chars().boxed())
                .map(intChar -> Character.toChars(intChar)[0]);

        assertEquals(Optional.empty(), charStream.next());

        eventStream.appendItem("abc");
        eventStream.appendItem("dd");

        assertEquals(Optional.of('a'), charStream.next());
        assertEquals(Optional.of('b'), charStream.next());
        assertEquals(Optional.of('c'), charStream.next());
        assertEquals(Optional.of('d'), charStream.next());
        assertEquals(Optional.of('d'), charStream.next());
        assertEquals(Optional.empty(), charStream.next());

        eventStream.appendItem("abc");
        eventStream.appendItem("dd");

        List<Character> seenItems = new ArrayList<>();

        assertThrows(NoSuchElementException.class, () -> {
            charStream.forEachBlocking(seenItems::add);
        });

        assertEquals(List.of('a', 'b', 'c', 'd', 'd'), seenItems);
    }
}
