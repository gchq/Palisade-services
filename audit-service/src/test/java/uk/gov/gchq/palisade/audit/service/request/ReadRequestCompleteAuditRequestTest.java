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
public class ReadRequestCompleteAuditRequestTest {
    @Rule
    public ExpectedException expectedEx = ExpectedException.none();
    private ReadRequestCompleteAuditRequest readRequestCompleteAuditRequest;

    @Before
    public void setUp() {
        readRequestCompleteAuditRequest = new ReadRequestCompleteAuditRequest();
    }

    @Test
    public void correctUser() {
        User actual = new User().userId("john");
        readRequestCompleteAuditRequest.setUser(actual);
        User expected = readRequestCompleteAuditRequest.getUser();
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void incorrectUser() {
        User actual = new User().userId("john");
        User processedUser = new User().userId("testUser1");
        readRequestCompleteAuditRequest.setUser(processedUser);
        User expected = readRequestCompleteAuditRequest.getUser();
        assertThat(actual, is(not(equalTo(expected))));
    }

    @Test
    public void nullUser() {
        expectedEx.expect(NullPointerException.class);
        expectedEx.expectMessage("The user type cannot be null");
        readRequestCompleteAuditRequest.setUser(null);
    }

    @Test
    public void validNumberOfRecordsReturned() {
        long actual = 12345678910L;
        readRequestCompleteAuditRequest.setNumberOfRecordsReturned(actual);
        long expected = readRequestCompleteAuditRequest.getNumberOfRecordsReturned();
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void negativeNumberOfRecordsReturned() {
        long actual = -1;
        readRequestCompleteAuditRequest.setNumberOfRecordsReturned(actual);
        long expected = readRequestCompleteAuditRequest.getNumberOfRecordsReturned();
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void invalidNumberOfRecordsReturned() {
        long actual = 100;
        long valueProcessed = 101;
        readRequestCompleteAuditRequest.setNumberOfRecordsReturned(valueProcessed);
        long expected = readRequestCompleteAuditRequest.getNumberOfRecordsReturned();
        assertThat(actual, is(not(equalTo(expected))));
    }

    @Test(expected = NumberFormatException.class)
    public void nullNumberOfRecordsReturned() {
        long actual = Integer.valueOf(null);
        readRequestCompleteAuditRequest.setNumberOfRecordsReturned(actual);
        long expected = readRequestCompleteAuditRequest.getNumberOfRecordsReturned();
        assertThat(actual, is(equalTo(expected)));
    }

    @Test
    public void equals() {
        ReadRequestCompleteAuditRequest o = readRequestCompleteAuditRequest.numberOfRecordsProcessed(100L)
                .numberOfRecordsReturned(200L)
                .context(new Context(Stream.of(new AbstractMap.SimpleImmutableEntry<String, Class<?>>("a string", String.class)).collect(toMap(AbstractMap.SimpleImmutableEntry::getKey, AbstractMap.SimpleImmutableEntry::getValue))))
                .rulesApplied(new Rules()).resource(new FileResource())
                .resource(new FileResource())
                .user(new User().userId("Person1"));
        boolean actual = readRequestCompleteAuditRequest.equals(o);
        assertThat(actual, is(true));
    }

}