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

package uk.gov.gchq.palisade.service.palisade.impl;

import org.mockito.Mockito;

import uk.gov.gchq.palisade.Generated;
import uk.gov.gchq.palisade.service.palisade.exception.NoCapacityException;
import uk.gov.gchq.palisade.service.palisade.request.ReadRequest;
import uk.gov.gchq.palisade.service.palisade.request.ReadResponse;
import uk.gov.gchq.palisade.service.palisade.service.DataService;

import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.CompletableFuture;

public class MockDataService implements DataService {
    private static DataService mock = Mockito.mock(DataService.class);

    public static DataService getMock() {
        return mock;
    }

    public static void setMock(final DataService mock) {
        if (null == mock) {
            MockDataService.mock = Mockito.mock(DataService.class);
        }
        MockDataService.mock = mock;
    }

    @Override
    public CompletableFuture<ReadResponse> read(final ReadRequest request) throws NoCapacityException {
        return mock.read(request);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", MockDataService.class.getSimpleName() + "[", "]")
                .toString();
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MockDataService)) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        final MockDataService that = (MockDataService) o;
        return Objects.equals(mock, that.mock);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(super.hashCode(), mock);
    }
}
