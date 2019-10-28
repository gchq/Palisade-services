/*
 * Copyright 2019 Crown Copyright
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

package uk.gov.gchq.palisade.service.palisade.repository;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.exception.NoConfigException;
import uk.gov.gchq.palisade.service.ServiceState;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@RunWith(JUnit4.class)
public class SimpleCacheServiceTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleCacheServiceTest.class);

    private SimpleCacheService cacheService = new SimpleCacheService();
    private ServiceState serviceState = new ServiceState();
    private BackingStore store = new HashMapBackingStore();
    private Duration maxLocalTTL = Duration.of(5, ChronoUnit.MINUTES);

    @Test(expected = NoConfigException.class)
    public void applyConfigFromReturnsErrorTest() {
        //Given
        serviceState.put("cache.svc.store", null);
        serviceState.put("cache.svc.max.ttl", Duration.of(5, ChronoUnit.MINUTES).toString());

        //When
        cacheService.maximumLocalCacheDuration(maxLocalTTL);
        cacheService.setBackingStore(store);
        cacheService.applyConfigFrom(serviceState);
    }
}
