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

package uk.gov.gchq.palisade.service.policy.request;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.time.Duration;
import java.util.Optional;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class AddCacheRequestTest {
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void timeToLive() {
        final AddCacheRequest addCacheRequest = new AddCacheRequest();
        //Given
        Optional<Duration> d = Optional.ofNullable(Duration.ofSeconds(100));
        //When
        addCacheRequest.timeToLive(d);
        //Then
        assertThat(addCacheRequest.getTimeToLive(), is(equalTo(d)));

        //And when
        thrown.expectMessage("negative time to live specified!");
        //Then
        addCacheRequest.timeToLive(Optional.ofNullable(Duration.ofSeconds(-33)));
    }

}