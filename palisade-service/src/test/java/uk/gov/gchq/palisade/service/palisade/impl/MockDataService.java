package uk.gov.gchq.palisade.service.palisade.impl;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.mockito.Mockito;

import uk.gov.gchq.palisade.ToStringBuilder;
import uk.gov.gchq.palisade.service.palisade.exception.NoCapacityException;
import uk.gov.gchq.palisade.service.palisade.request.ReadRequest;
import uk.gov.gchq.palisade.service.palisade.request.ReadResponse;
import uk.gov.gchq.palisade.service.palisade.service.DataService;

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
    public String toString() {
        return new ToStringBuilder(this)
                .toString();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(7, 11)
                .toHashCode();
    }
}
