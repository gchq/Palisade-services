package uk.gov.gchq.palisade.service.resource.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.assertj.core.api.Assertions.assertThat;

class FunctionalIteratorTest {

    FunctionalIterator<Integer> testIterator;

    @BeforeEach
    void setUp() {
        testIterator = FunctionalIterator.fromIterator(List.of(0, 1, 2, 3, 4, 5).iterator());
    }

    @Test
    void testMap() {
        FunctionalIterator<Integer> dslTest = testIterator
                .map(i -> i + 1);
        assertThat(listOf(dslTest)).isEqualTo(List.of(1, 2, 3, 4, 5, 6));
    }

    @Test
    void testFilterFirst() {
        FunctionalIterator<Integer> dslTest = testIterator
                .filter(i -> i == 0);
        assertThat(listOf(dslTest)).isEqualTo(List.of(0));
    }

    @Test
    void testFilterLast() {
        FunctionalIterator<Integer> dslTest = testIterator
                .filter(i -> i == 5);
        assertThat(listOf(dslTest)).isEqualTo(List.of(5));
    }

    @Test
    void testMapLast() {
        FunctionalIterator<Integer> dslTest = testIterator
                .mapLast(i -> 100);
        assertThat(listOf(dslTest)).isEqualTo(List.of(0, 1, 2, 3, 4, 100));
    }

    @Test
    void testTwoFlatMap() {
        FunctionalIterator<Integer> dslTest = testIterator
                .flatMap(i -> List.of(i, 2 * i).iterator());
        assertThat(listOf(dslTest)).isEqualTo(List.of(0, 0, 1, 2, 2, 4, 3, 6, 4, 8, 5, 10));
    }

    @Test
    void testZeroFlatMap() {
        FunctionalIterator<Integer> dslTest = testIterator
                .flatMap(i -> Collections.emptyIterator());
        assertThat(listOf(dslTest)).isEqualTo(Collections.emptyList());
    }

    @Test
    void testPeek() {
        final AtomicInteger count = new AtomicInteger(0);
        FunctionalIterator<Integer> dslTest = testIterator
                .peek(i -> count.incrementAndGet());
        assertThat(listOf(dslTest)).isEqualTo(List.of(0, 1, 2, 3, 4, 5));
        assertThat(count.intValue()).isEqualTo(6);
    }

    @Test
    void testWholeDSLTwice() {
        AtomicInteger firstSum = new AtomicInteger(0);
        FunctionalIterator<Integer> dslTest = testIterator
                .map(i -> i + 1) // 1, 2, 3, 4, 5, 6
                .filter(i -> i % 3 != 0 && i % 2 != 0) // 1, 5
                .mapLast(i -> i + 2) // 1, 7
                .flatMap(i -> List.of(i, i*i).iterator()) // 1, 1, 7, 49
                .peek(firstSum::addAndGet) // 58
                .map(i -> i - 1) // 0, 0, 6, 48
                .mapLast(i -> i - 40) // 0, 0, 6, 8
                .filter(i -> i > 0 && i < 8) // 6
                .flatMap(i -> List.of(i * (i + 1)).iterator()) // 42
                .peek(System.out::println); // Print 1
        assertThat(listOf(dslTest)).isEqualTo(List.of(42));
    }

    private <T> List<T> listOf(final Iterator<T> iterator) {
        Iterable<T> iterable = () -> iterator;
        return StreamSupport
                .stream(iterable.spliterator(), false)
                .collect(Collectors.toList());
    }
}