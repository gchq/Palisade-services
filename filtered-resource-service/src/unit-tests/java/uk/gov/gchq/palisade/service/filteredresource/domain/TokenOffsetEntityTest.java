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

package uk.gov.gchq.palisade.service.filteredresource.domain;

import org.junit.jupiter.api.Test;

import uk.gov.gchq.palisade.service.filteredresource.ApplicationTestData;
import uk.gov.gchq.palisade.service.filteredresource.config.RedisTtlConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class TokenOffsetEntityTest {
    /**
     * Expose a setter for the internal KEYSPACE_TTL {@link java.util.Map}
     */
    static class RedisTtlConfigurationProxy extends RedisTtlConfiguration {
        static void setTimeToLiveSeconds(final String keyspace, final Long ttlSeconds) {
            KEYSPACE_TTL.put(keyspace, ttlSeconds);
        }
    }

    @Test
    void testEntityAcquiresRedisTtl() {
        // Given
        long entityTtl = 123L;
        RedisTtlConfigurationProxy.setTimeToLiveSeconds("TokenOffsetEntity", entityTtl);

        // When
        var entity = new TokenOffsetEntity(ApplicationTestData.REQUEST_TOKEN, ApplicationTestData.OFFSET);

        // Then
        assertThat(entity.getTimeToLive())
                .as("Check that the Redis TTL is acquired")
                .isEqualTo(entityTtl);
    }
}
