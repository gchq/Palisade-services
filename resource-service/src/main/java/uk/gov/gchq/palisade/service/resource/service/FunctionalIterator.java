/*
 * Copyright 2020 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.gchq.palisade.service.resource.service;

import java.io.Closeable;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * An interface that allows an {@link Iterator} to have {@link java.util.stream.Stream}-like methods.
 *
 * @param <T> the type of the {@link FunctionalIterator}
 */
public interface FunctionalIterator<T> extends Iterator<T>, AutoCloseable {

    /**
     * A method used to create a {@link FunctionalIterator} from an iterator.
     *
     * @param iterator the Iterator used to create a FunctionalIterator
     * @param <T>      the type of the Iterator
     * @return a {@link FunctionalIterator} of type {@link T}
     */
    static <T> FunctionalIterator<T> fromIterator(final Iterator<T> iterator) {
        return new PlainIterator<>(iterator);
    }

    /**
     * A method that maps an object within the iterator.
     *
     * @param map the function used to perform the map
     * @param <R> the type of the Iterator
     * @return a {@link FunctionalIterator} of type {@link R}
     */
    default <R> FunctionalIterator<R> map(final Function<T, R> map) {
        return new MapIterator<>(this, map);
    }

    /**
     * A method that maps the last element of the iterator.
     *
     * @param mapLast the unary operator used to perform the map.
     * @return a {@link FunctionalIterator} of type {@link T}
     */
    default FunctionalIterator<T> mapLast(final UnaryOperator<T> mapLast) {
        return new MapLastIterator<>(this, mapLast);
    }

    /**
     * A method used to filter out any elements that do not match a {@link Predicate}.
     *
     * @param filter the predicate used to filter out any elements
     * @return a {@link FunctionalIterator} of type {@link T}
     */
    default FunctionalIterator<T> filter(final Predicate<T> filter) {
        return new FilterIterator<>(this, filter);
    }

    /**
     * A method used to map an iterator
     *
     * @param flatMap the function used to perform the flatMap
     * @param <R>     the type of the iterator
     * @return a {@link FunctionalIterator} of type {@link R}
     */
    default <R> FunctionalIterator<R> flatMap(final Function<T, Iterator<R>> flatMap) {
        return new FlatMapIterator<>(this, flatMap);
    }

    /**
     * A method that peeks into the elements of the iterator.
     *
     * @param peek the consumer used to peek into the iterator elements
     * @return a {@link FunctionalIterator} of type {@link T}
     */
    default FunctionalIterator<T> peek(final Consumer<T> peek) {
        return new PeekIterator<>(this, peek);
    }

    /**
     * A standard implementation of a {@link FunctionalIterator}.
     *
     * @param <T> the type of the iterator
     */
    class PlainIterator<T> implements FunctionalIterator<T> {

        private final Iterator<T> delegate;

        public PlainIterator(final Iterator<T> delegate) {
            this.delegate = delegate;
        }

        @Override
        public boolean hasNext() {
            return this.delegate.hasNext();
        }

        @Override
        public T next() {
            return this.delegate.next();
        }

        @Override
        public void close() {
            // Do nothing
        }
    }

    /**
     * A {@link FunctionalIterator} implementation that allows the iterator to map an object.
     *
     * @param <T> the original type of the iterator
     * @param <R> the returned type of the iterator
     */
    class MapIterator<T, R> implements FunctionalIterator<R> {

        private final FunctionalIterator<T> delegate;
        private final Function<T, R> map;

        public MapIterator(final FunctionalIterator<T> delegate, final Function<T, R> map) {
            this.delegate = delegate;
            this.map = map;
        }

        @Override
        public boolean hasNext() {
            return this.delegate.hasNext();
        }

        @Override
        public R next() {
            return this.map.apply(this.delegate.next());
        }

        @Override
        public void close() throws Exception {
            this.delegate.close();
        }
    }

    /**
     * A {@link FunctionalIterator} implementation that allows an iterator to map the last element.
     *
     * @param <T> the type of the iterator
     */
    class MapLastIterator<T> implements FunctionalIterator<T> {

        private final FunctionalIterator<T> delegate;
        private final UnaryOperator<T> mapLast;

        public MapLastIterator(final FunctionalIterator<T> delegate, final UnaryOperator<T> mapLast) {
            this.delegate = delegate;
            this.mapLast = mapLast;
        }

        @Override
        public boolean hasNext() {
            return this.delegate.hasNext();
        }

        @Override
        public T next() {
            T next = this.delegate.next();
            if (!this.delegate.hasNext()) {
                next = this.mapLast.apply(next);
            }
            return next;
        }

        @Override
        public void close() throws Exception {
            this.delegate.close();
        }
    }

    /**
     * A {@link FunctionalIterator} implementation that allows an iterator to filter out elements
     *
     * @param <T> the type of the iterator
     */
    class FilterIterator<T> implements FunctionalIterator<T> {

        private final FunctionalIterator<T> delegate;
        private final Predicate<T> filter;
        private boolean bufferExhausted;
        private T buffer;

        public FilterIterator(final FunctionalIterator<T> delegate, final Predicate<T> filter) {
            this.delegate = delegate;
            this.filter = filter;
            this.bufferExhausted = true;
            this.prebuffer();
        }

        @Override
        public boolean hasNext() {
            return !this.bufferExhausted;
        }

        @Override
        public T next() {
            if (!this.bufferExhausted) {
                if (!this.delegate.hasNext()) {
                    this.bufferExhausted = true;
                }

                T elem = this.buffer;
                this.rebuffer();
                if (!this.filter.test(this.buffer)) {
                    this.bufferExhausted = true;
                }

                return elem;
            } else {
                throw new NoSuchElementException();
            }
        }

        private void prebuffer() {
            if (this.delegate.hasNext()) {
                this.buffer = this.delegate.next();
                if (!this.filter.test(this.buffer)) {
                    this.rebuffer();
                }

                this.bufferExhausted = !this.filter.test(this.buffer);
            }
        }

        private void rebuffer() {
            if (this.delegate.hasNext()) {
                do {
                    this.buffer = this.delegate.next();
                } while (!this.filter.test(this.buffer) && this.delegate.hasNext());
            }
        }

        @Override
        public void close() throws Exception {
            this.delegate.close();
        }
    }

    /**
     * A {@link FunctionalIterator} implementation that allows an iterator to flat map the elements
     *
     * @param <T> the original type of the iterator
     * @param <R> the returned type of the iterator
     */
    class FlatMapIterator<T, R> implements FunctionalIterator<R> {

        private final FunctionalIterator<T> delegate;
        private final Function<T, Iterator<R>> flatMap;
        private Iterator<R> buffer;

        public FlatMapIterator(final FunctionalIterator<T> delegate, final Function<T, Iterator<R>> flatMap) {
            this.delegate = delegate;
            this.flatMap = flatMap;
            this.prebuffer();
        }

        @Override
        public boolean hasNext() {
            return this.buffer.hasNext();
        }

        @Override
        public R next() {
            R next = this.buffer.next();
            if (!this.buffer.hasNext()) {
                this.rebuffer();
            }
            return next;
        }

        private void prebuffer() {
            if (this.delegate.hasNext()) {
                this.buffer = this.flatMap.apply(this.delegate.next());
            }
            this.rebuffer();
        }

        private void rebuffer() {
            while (!this.buffer.hasNext() && this.delegate.hasNext()) {
                this.buffer = this.flatMap.apply(this.delegate.next());
            }
        }

        @Override
        public void close() throws Exception {
            this.delegate.close();
        }
    }

    /**
     * A {@link FunctionalIterator} implementation that allows an iterator to peek into the elements
     *
     * @param <T> the type of the iterator
     */
    class PeekIterator<T> implements FunctionalIterator<T> {

        private final FunctionalIterator<T> delegate;
        private final Consumer<T> peek;

        public PeekIterator(final FunctionalIterator<T> delegate, final Consumer<T> peek) {
            this.delegate = delegate;
            this.peek = peek;
        }

        @Override
        public boolean hasNext() {
            return this.delegate.hasNext();
        }

        @Override
        public T next() {
            T next = this.delegate.next();
            this.peek.accept(next);
            return next;
        }

        @Override
        public void close() throws Exception {
            this.delegate.close();
        }
    }
}
