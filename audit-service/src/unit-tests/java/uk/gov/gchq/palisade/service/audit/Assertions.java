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
package uk.gov.gchq.palisade.service.audit;

import org.mockito.ArgumentCaptor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.http.ResponseEntity;

/**
 * Factory methods for custom assertions within the audit service test framework
 */
public class Assertions {

    /**
     * Returns a new HealthAssert with the provided actual {@code health}
     *
     * @param health the actual value
     * @return a new HealthAssert
     */
    public static HealthAssert assertThat(final Health health) {
        return new HealthAssert(health);
    }

    /**
     * Returns a new ArgumentCaptorAssert with the provided actual
     * {@code argumentCaptor}
     *
     * @param argumentCaptor the actual value
     * @return a new ArgumentCaptorAssert
     */
    public static <T> ArgumentCaptorAssert<T> assertThat(final ArgumentCaptor<T> argumentCaptor) {
        return new ArgumentCaptorAssert<>(argumentCaptor);
    }

    /**
     * Returns a new ResponseEntityAssert with the provided actual
     * {@code responseEntity}
     *
     * @param responseEntity the actual value
     * @return a new ResponseEntityAssert
     */
    public static <T> ResponseEntityAssert<T> assertThat(final ResponseEntity<T> responseEntity) {
        return new ResponseEntityAssert<>(responseEntity);
    }

}