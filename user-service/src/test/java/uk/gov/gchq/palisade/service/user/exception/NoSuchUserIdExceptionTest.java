package uk.gov.gchq.palisade.service.user.exception;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class NoSuchUserIdExceptionTest {


    @Test
    public void NoSuchUserIdExceptionTest() {
        final NoSuchUserIdException noSuchUserIdException = new NoSuchUserIdException("NoSuchUser");
        assertThat(noSuchUserIdException.getMessage(), is(equalTo("NoSuchUser")));

    }
}
