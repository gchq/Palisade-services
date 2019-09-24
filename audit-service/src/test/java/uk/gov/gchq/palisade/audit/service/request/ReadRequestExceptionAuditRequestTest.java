package uk.gov.gchq.palisade.audit.service.request;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.rule.Rules;

import java.util.AbstractMap;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;

@RunWith(SpringRunner.class)
public class ReadRequestExceptionAuditRequestTest {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();
    private ReadRequestExceptionAuditRequest readRequestExceptionAuditRequest;

    @Before
    public void setUp() {
        readRequestExceptionAuditRequest = new ReadRequestExceptionAuditRequest();
    }

    @Test
    public void validToken() {
        String actual = "String1";
        readRequestExceptionAuditRequest.setToken(actual);
        String expected = readRequestExceptionAuditRequest.getToken();
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void invalidToken() {
        String actual = "String1";
        readRequestExceptionAuditRequest.setToken("String2");
        String expected = readRequestExceptionAuditRequest.getToken();
        assertThat(actual, is(not(equalTo(expected))));
    }

    @Test
    public void nullToken() {
        expectedEx.expect(NullPointerException.class);
        expectedEx.expectMessage("The token cannot be null");
        readRequestExceptionAuditRequest.setToken(null);
    }

    @Test
    public void validException() {
        Throwable actual = new Throwable("Error");
        readRequestExceptionAuditRequest.setException(actual);
        Throwable expected = readRequestExceptionAuditRequest.getException();
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void invalidException() {
        Throwable actual = new Throwable("Error");
        Throwable processed = new Throwable("Error2");
        readRequestExceptionAuditRequest.setException(processed);
        Throwable expected = readRequestExceptionAuditRequest.getException();
        assertThat(actual, is(not(equalTo(expected))));
    }

    @Test
    public void nullException() {
        expectedEx.expect(NullPointerException.class);
        expectedEx.expectMessage("The exception cannot be null");
        readRequestExceptionAuditRequest.setException(null);
    }

    @Test
    public void equals() {
        ReadRequestExceptionAuditRequest o = readRequestExceptionAuditRequest.exception(new Throwable("Error"))
                .token("String1")
                .resource(new FileResource());
        boolean actual = readRequestExceptionAuditRequest.equals(o);
        assertThat(actual, is(true));
    }
}