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

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Mock
    private RequestId requestId;

    @DataPoints
    public static final CacheRequest[] cacheRequests = new CacheRequest[] {
            new AddCacheRequest<String>(),
            new GetCacheRequest<String>(),
            new ListCacheRequest(),
            new RemoveCacheRequest()
    };

    static {
        for (CacheRequest cacheRequest : cacheRequests) {
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

    @Theory
    public void setOriginalRequestIdThrows(CacheRequest cacheRequest) {
        // Then (expected)
        thrown.expect(ForbiddenException.class);

        // When
        cacheRequest.setOriginalRequestId(requestId);
    }

    @Theory
    public void getOriginalRequestIdThrows(CacheRequest cacheRequest) {
        // Then (expected)
        thrown.expect(ForbiddenException.class);

        // When
        cacheRequest.getOriginalRequestId();
    }

    @Theory
    public void reflexiveEquals(CacheRequest x) {
        // Then
        assertThat(x, equalTo(x));
    }

    @Theory
    public void nullNotEquals(CacheRequest x) {
        // Then
        assertThat(x, not(equalTo(nullValue())));
    }

    @Theory
    public void symmetricEquals(CacheRequest x, CacheRequest y) {
        // Given
        assumeThat(x, equalTo(y));
        // Then
        assertThat(y, equalTo(x));
    }

    @Theory
    public void transitiveEquals(CacheRequest x, CacheRequest y, CacheRequest z) {
        // Given
        assumeThat(x, equalTo(y));
        assumeThat(y, equalTo(z));
        // Then
        assertThat(x, equalTo(z));
    }

    @Theory
    public void consistentHashCode(CacheRequest x) {
        // Then
        assertThat(x.hashCode(), equalTo(x.hashCode()));
    }

    @Theory
    public void equalHashCodeWhenEqual(CacheRequest x, CacheRequest y) {
        // Given
        assumeThat(x, equalTo(y));
        // Then
        assertThat(x.hashCode(), equalTo(y.hashCode()));
    }
}
