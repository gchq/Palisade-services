package uk.gov.gchq.palisade.service.user.request;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.UserId;

import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;

@RunWith(JUnit4.class)
public class GetUserRequestTest {
    public final ObjectMapper mapper = new ObjectMapper();


    @Test
    public void GetUserRequestTest() {
        final GetUserRequest subject = GetUserRequest.create(new RequestId().id("newId")).withUserId(new UserId().id("newUser"));
        assertThat("GetUserRequest not constructed", subject.userId.getId(), is(equalTo("newUser")));
    }

    @Test
    public void GetUserRequestToJsonTest() throws IOException {
        final GetUserRequest subject = GetUserRequest.create(new RequestId().id("newId"))
                .withUserId(new UserId().id("user"));

        final JsonNode asNode = this.mapper.readTree(this.mapper.writeValueAsString(subject));
        final Iterable<String> iterable = asNode::fieldNames;

        assertThat("GetUserRequest not parsed to json", StreamSupport.stream(iterable.spliterator(), false).collect(Collectors.joining(", ")), is(equalTo("id, originalRequestId, user")));
    }

    @Test
    public void GetUserRequestFromJsonTest() throws IOException {
        final GetUserRequest subject = GetUserRequest.create(new RequestId().id("123"))
                .withUserId(new UserId().id("newUser"));

        final String jsonString = "{\"id\":{\"id\":\"9b3b4751-d88d-4aad-9a59-022fb76e8474\"},\"originalRequestId\":{\"id\":\"123\"},\"userId\":{\"id\":\"newUser\"}}";

        assertThat("GetUserRequest could not be parsed from json string", subject.userId, is(equalTo(new UserId().id("newUser"))));
    }
}
