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

import java.util.AbstractMap;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNot.not;

@RunWith(SpringRunner.class)
public class RegisterRequestCompleteAuditRequestTest {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();
    private RegisterRequestCompleteAuditRequest registerRequestCompleteAuditRequest;

    @Before
    public void setUp() {
        registerRequestCompleteAuditRequest = new RegisterRequestCompleteAuditRequest();
    }

    @Test
    public void correctUser() {
        User actual = new User().userId("testUser1");
        registerRequestCompleteAuditRequest.setUser(actual);
        User expected = registerRequestCompleteAuditRequest.getUser();
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void incorrectUser() {
        User actual = new User().userId("testUser1");
        User processedUser = new User().userId("testUser2");
        registerRequestCompleteAuditRequest.setUser(processedUser);
        User expected = registerRequestCompleteAuditRequest.getUser();
        assertThat(actual, is(not(equalTo(expected))));
    }

    @Test
    public void nullUser() {
        expectedEx.expect(NullPointerException.class);
        expectedEx.expectMessage("The user type cannot be null");
        registerRequestCompleteAuditRequest.setUser(null);
    }

    @Test
    public void validContext() {
        Context actual = new Context().purpose("SALARY");
        registerRequestCompleteAuditRequest.setContext(actual);
        Context expected = registerRequestCompleteAuditRequest.getContext();
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void invalidContext() {
        Context actual = new Context().purpose("SALARY");
        Context processed = new Context().purpose("HR");
        registerRequestCompleteAuditRequest.setContext(processed);
        Context expected = registerRequestCompleteAuditRequest.getContext();
        assertThat(actual, is(not(equalTo(expected))));
    }

    @Test
    public void nullContext() {
        expectedEx.expect(NullPointerException.class);
        expectedEx.expectMessage("The context cannot be set to null");
        registerRequestCompleteAuditRequest.setContext(null);
    }

    @Test
    public void equals() {
        RegisterRequestCompleteAuditRequest o = registerRequestCompleteAuditRequest
                .context(new Context(Stream.of(new AbstractMap.SimpleImmutableEntry<String, Class<?>>("a string", String.class)).collect(toMap(AbstractMap.SimpleImmutableEntry::getKey, AbstractMap.SimpleImmutableEntry::getValue))))
                .user(new User().userId("Person1"))
                .leafResources(Stream.of(new FileResource()).collect(toSet()));
        boolean actual = registerRequestCompleteAuditRequest.equals(o);
        assertThat(actual, is(true));
    }
}