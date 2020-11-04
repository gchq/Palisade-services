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

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public interface FunctionalIterator<T> extends Iterator<T> {
    static <T> FunctionalIterator<T> fromIterator(final Iterator<T> iterator) {
        return new PlainIterator<>(iterator);
    }

    default <R> FunctionalIterator<R> map(final Function<T, R> map) {
        return new MapIterator<>(this, map);
    }

    default FunctionalIterator<T> mapLast(final UnaryOperator<T> mapLast) {
        return new MapLastIterator<>(this, mapLast);
    }

    default FunctionalIterator<T> filter(final Predicate<T> filter) {
        return new FilterIterator<>(this, filter);
    }

    default <R> FunctionalIterator<R> flatMap(final Function<T, Iterator<R>> flatMap) {
        return new FlatMapIterator<>(this, flatMap);
    }

    default FunctionalIterator<T> peek(final Consumer<T> peek) {
        return new PeekIterator<>(this, peek);
    }

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
    }

    class MapIterator<T, R> implements FunctionalIterator<R> {

        private final Iterator<T> delegate;
        private final Function<T, R> map;

        public MapIterator(final Iterator<T> delegate, final Function<T, R> map) {
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
    }

    class MapLastIterator<T> implements FunctionalIterator<T> {

        private final Iterator<T> delegate;
        private final UnaryOperator<T> mapLast;

        public MapLastIterator(final Iterator<T> delegate, final UnaryOperator<T> mapLast) {
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
    }

    class FilterIterator<T> implements FunctionalIterator<T> {

        private final Iterator<T> delegate;
        private final Predicate<T> filter;
        private boolean bufferExhausted;
        private T buffer;

        public FilterIterator(final Iterator<T> delegate, final Predicate<T> filter) {
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
    }

    class FlatMapIterator<T, R> implements FunctionalIterator<R> {

        private final Iterator<T> delegate;
        private final Function<T, Iterator<R>> flatMap;
        private Iterator<R> buffer;

        public FlatMapIterator(final Iterator<T> delegate, final Function<T, Iterator<R>> flatMap) {
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
    }

    class PeekIterator<T> implements FunctionalIterator<T> {
        private final Iterator<T> delegate;
        private final Consumer<T> peek;
        public PeekIterator(final Iterator<T> delegate, final Consumer<T> peek) {
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
    }
}
