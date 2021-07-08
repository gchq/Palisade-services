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

package uk.gov.gchq.palisade.service.audit.web;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A health indicator for the Serialising and Deserialising of audit messages. If there are no logged exceptions
 * then the service is healthy, otherwise mark the service has unhealthy.
 */
public class SerDesHealthIndicator implements HealthIndicator {

    /**
     * Serialisation exceptions
     */
    protected static final Queue<Exception> SER_DES_EXCEPTIONS = new ConcurrentLinkedQueue<>();

    /**
     * Adds any encountered serialisation exceptions to the {@link Queue} of exceptions
     *
     * @param exception the encountered exception
     */
    public static void addSerDesExceptions(final Exception exception) {
        SER_DES_EXCEPTIONS.add(exception);
    }

    @Override
    public Health health() {

        if (SER_DES_EXCEPTIONS.isEmpty()) {
            return Health.up()
                    .build();
        }

        return Health.down()
                .withDetail("errors", SER_DES_EXCEPTIONS)
                .build();
    }
}
