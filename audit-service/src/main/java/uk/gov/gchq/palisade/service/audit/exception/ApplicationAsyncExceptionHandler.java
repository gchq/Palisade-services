/*
 * Copyright 2018-2021 Crown Copyright
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

package uk.gov.gchq.palisade.service.audit.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.lang.Nullable;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A class that handles the Uncaught Exceptions thrown by the Async processes
 */
public class ApplicationAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationAsyncExceptionHandler.class);

    @Override
    public void handleUncaughtException(final Throwable throwable, final Method method, @Nullable final Object[] objects) {
        String parameters = Stream.of(Optional.ofNullable(objects).orElseGet(() -> new Object[0]))
                .map(Object::toString)
                .collect(Collectors.joining(", ", "[", "]"));
        LOGGER.error("Uncaught Exception thrown by Async method [{}] : {} with Parameters: {}", method.getName(), throwable.getMessage(), parameters);
    }
}
