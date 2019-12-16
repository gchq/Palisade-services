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