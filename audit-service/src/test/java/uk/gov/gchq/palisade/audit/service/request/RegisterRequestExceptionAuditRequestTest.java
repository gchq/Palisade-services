package uk.gov.gchq.palisade.audit.service.request;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.UserId;

import java.util.AbstractMap;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;

@RunWith(SpringRunner.class)
public class RegisterRequestExceptionAuditRequestTest {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();
    private RegisterRequestExceptionAuditRequest registerRequestExceptionAuditRequest;

    @Before
    public void setUp() {
        registerRequestExceptionAuditRequest = new RegisterRequestExceptionAuditRequest();
    }

    @Test
    public void correctUser() {
        UserId actual = new UserId().id("testUser1");
        registerRequestExceptionAuditRequest.setUserId(actual);
        UserId expected = registerRequestExceptionAuditRequest.getUserId();
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void incorrectUser() {
        UserId actual = new UserId().id("testUser1");
        UserId processedUser = new UserId().id("testUser2");
        registerRequestExceptionAuditRequest.setUserId(processedUser);
        UserId expected = registerRequestExceptionAuditRequest.getUserId();
        assertThat(actual, is(not(equalTo(expected))));
    }

    @Test
    public void nullUser() {
        expectedEx.expect(NullPointerException.class);
        expectedEx.expectMessage("The userId cannot be null");
        registerRequestExceptionAuditRequest.setUserId(null);
    }

    @Test
    public void correctResource() {
        String actual = "String1";
        registerRequestExceptionAuditRequest.setResourceId(actual);
        String expected = registerRequestExceptionAuditRequest.getResourceId();
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void incorrectResource() {
        String actual = "String1";
        String processed = "String2";
        registerRequestExceptionAuditRequest.setResourceId(processed);
        String expected = registerRequestExceptionAuditRequest.getResourceId();
        assertThat(actual, is(not(equalTo(expected))));
    }

    @Test
    public void nullResource() {
        expectedEx.expect(NullPointerException.class);
        expectedEx.expectMessage("The resourceId cannot be null");
        registerRequestExceptionAuditRequest.setResourceId(null);
    }

    @Test
    public void validContext() {
        Context actual = new Context().purpose("SALARY");
        registerRequestExceptionAuditRequest.setContext(actual);
        Context expected = registerRequestExceptionAuditRequest.getContext();
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void invalidContext() {
        Context actual = new Context().purpose("SALARY");
        Context processed = new Context().purpose("HR");
        registerRequestExceptionAuditRequest.setContext(processed);
        Context expected = registerRequestExceptionAuditRequest.getContext();
        assertThat(actual, is(not(equalTo(expected))));
    }

    @Test
    public void nullContext() {
        expectedEx.expect(NullPointerException.class);
        expectedEx.expectMessage("The context cannot be set to null");
        registerRequestExceptionAuditRequest.setContext(null);
    }

    @Test
    public void validException() {
        Throwable actual = new Throwable("Error");
        registerRequestExceptionAuditRequest.setException(actual);
        Throwable expected = registerRequestExceptionAuditRequest.getException();
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void invalidException() {
        Throwable actual = new Throwable("Error");
        Throwable processed = new Throwable("Error2");
        registerRequestExceptionAuditRequest.setException(processed);
        Throwable expected = registerRequestExceptionAuditRequest.getException();
        assertThat(actual, is(not(equalTo(expected))));
    }

    @Test
    public void nullException() {
        expectedEx.expect(NullPointerException.class);
        expectedEx.expectMessage("The exception type cannot be null");
        registerRequestExceptionAuditRequest.setException(null);
    }

    @Test
    public void equals() {
        RegisterRequestExceptionAuditRequest o = registerRequestExceptionAuditRequest
                .context(new Context(Stream.of(new AbstractMap.SimpleImmutableEntry<String, Class<?>>("a string", String.class)).collect(toMap(AbstractMap.SimpleImmutableEntry::getKey, AbstractMap.SimpleImmutableEntry::getValue))))
                .exception(new Throwable("Error1"))
                .resourceId("String1")
                .userId(new UserId().id("User1"));
        boolean actual = registerRequestExceptionAuditRequest.equals(o);
        assertThat(actual, is(true));
    }

}