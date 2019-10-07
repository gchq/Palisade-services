package uk.gov.gchq.palisade.service.user.request;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.User;

import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

@RunWith(JUnit4.class)
public class AddUserRequestTest {
    public final ObjectMapper mapper = new ObjectMapper();


    @Test
    public void AddUserRequestTest() {
        final AddUserRequest subject = AddUserRequest.create(new RequestId().id("newId")).withUser(new User().userId("newUser"));
        assertThat("AddUserRequest not constructed", subject.user.getUserId().getId(), is(equalTo("newUser")));
    }

    @Test
    public void AddUserRequestToJsonTest() throws IOException {
        final AddUserRequest subject = AddUserRequest.create(new RequestId().id("newId"))
                .withUser(new User().userId("user"));

        final JsonNode asNode = this.mapper.readTree(this.mapper.writeValueAsString(subject));
        final Iterable<String> iterable = asNode::fieldNames;

        assertThat("AddUserRequest not parsed to json", StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.joining(", ")), is(equalTo("originalRequestId, user, id")));
    }

    @Test
    public void AddUserRequestFromJsonTest() throws IOException {
        final AddUserRequest subject = AddUserRequest.create(new RequestId().id("123"))
                .withUser(new User().userId("user"));

        final String jsonString = "{\"id\":{\"id\":\"3c6324a2-3dfa-43c8-9d96-576b558e2169\"},\"originalRequestId\":{\"id\":\"123\"},\"user\":{\"userId\":{\"id\":\"user\"},\"roles\":[],\"auths\":[],\"class\":\"uk.gov.gchq.palisade.User\"}}}";
        final String asNode = this.mapper.readTree(this.mapper.writeValueAsString(subject)).toString();

        final AddUserRequest result = this.mapper.readValue(jsonString, AddUserRequest.class);

        assertThat("AddUserRequest could not be parsed from json string", subject.user, is(equalTo(new User().userId("user"))));
    }
}
