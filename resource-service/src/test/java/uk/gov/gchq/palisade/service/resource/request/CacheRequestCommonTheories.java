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


package uk.gov.gchq.palisade.service.resource.request;

import org.junit.Rule;
import org.junit.experimental.theories.DataPoints;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.exception.ForbiddenException;
import uk.gov.gchq.palisade.service.resource.impl.MockDataService;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assume.assumeThat;

@RunWith(Theories.class)
public class CacheRequestCommonTheories {

    //@DataPoints requires the property to be public
    @SuppressWarnings("checkstyle:visibilitymodifier")
    @DataPoints
    public static final CacheRequest[] CACHE_REQUESTS = new CacheRequest[]{
            new AddCacheRequest<String>(),
            new GetCacheRequest<String>(),
            new ListCacheRequest(),
            new RemoveCacheRequest()
    };

    static {
        for (CacheRequest cacheRequest : CACHE_REQUESTS) {
            if (cacheRequest instanceof ListCacheRequest) {
                ((ListCacheRequest) cacheRequest).prefix("test key");
            } else {
                cacheRequest.setKey("test key");
            }
            cacheRequest.service(MockDataService.getMock().getClass());
            if (cacheRequest instanceof AddCacheRequest) {
                ((AddCacheRequest<String>) cacheRequest).setValue("test value");
            }
        }
    }

    @Rule
    public ExpectedException thrown = ExpectedException.none();
    @Mock
    private RequestId requestId;

    @Theory
    public void setOriginalRequestIdThrows(final CacheRequest cacheRequest) {
        // Then (expected)
        thrown.expect(ForbiddenException.class);

        // When
        cacheRequest.setOriginalRequestId(requestId);
    }

    @Theory
    public void getOriginalRequestIdThrows(final CacheRequest cacheRequest) {
        // Then (expected)
        thrown.expect(ForbiddenException.class);

        // When
        cacheRequest.getOriginalRequestId();
    }

    @Theory
    public void reflexiveEquals(final CacheRequest x) {
        // Then
        assertThat(x, equalTo(x));
    }

    @Theory
    public void nullNotEquals(final CacheRequest x) {
        // Then
        assertThat(x, not(equalTo(nullValue())));
    }

    @Theory
    public void symmetricEquals(final CacheRequest x, final CacheRequest y) {
        // Given
        assumeThat(x, equalTo(y));
        // Then
        assertThat(y, equalTo(x));
    }

    @Theory
    public void transitiveEquals(final CacheRequest x, final CacheRequest y, final CacheRequest z) {
        // Given
        assumeThat(x, equalTo(y));
        assumeThat(y, equalTo(z));
        // Then
        assertThat(x, equalTo(z));
    }

    @Theory
    public void consistentHashCode(final CacheRequest x) {
        // Then
        assertThat(x.hashCode(), equalTo(x.hashCode()));
    }

    @Theory
    public void equalHashCodeWhenEqual(final CacheRequest x, final CacheRequest y) {
        // Given
        assumeThat(x, equalTo(y));
        // Then
        assertThat(x.hashCode(), equalTo(y.hashCode()));
    }
}
