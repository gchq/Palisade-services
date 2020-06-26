package uk.gov.gchq.palisade.service.policy.request;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.service.SimpleConnectionDetail;
import uk.gov.gchq.palisade.service.policy.response.common.domain.User;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@JsonTest
public class PolicyRequestTest {

    @Autowired
    private JacksonTester<PolicyRequest> jacksonTester;


    /**
     * Create the object using the builder and then serialise it to a Json string. Test the content of the Json string
     *
     * @throws IOException if it fails to parse the object
     */
    @Test
    public void testSerialiseResourceResponseToJson() throws IOException {

        Context context = new Context().purpose("testContext");
        User user = User.create("testUserId");
        LeafResource resource = new FileResource().id("/test/file.format")
                .type("java.lang.String")
                .serialisedFormat("format")
                .connectionDetail(new SimpleConnectionDetail().serviceName("test-service"))
                .parent(new SystemResource().id("/test"));
        PolicyRequest policyRequest = PolicyRequest.Builder.create().withContext(context).withUser(user).withResource(resource);

        JsonContent<PolicyRequest> policyRequestJsonContent = jacksonTester.write(policyRequest);

        assertThat(policyRequestJsonContent).extractingJsonPathStringValue("$.user.user_id").isEqualTo("testUserId");
        assertThat(policyRequestJsonContent).extractingJsonPathStringValue("$.context.contents.purpose").isEqualTo("testContext");
        assertThat(policyRequestJsonContent).extractingJsonPathStringValue("$.resource.id").isEqualTo("/test/file.format");

    }

    /**
     * Create the ResourceResponse object from a Json string and then test the content of the object.
     *
     * @throws IOException if it fails to parse the string into an object
     */
    @Test
    public void testDeserializeJsonToResourceResponse() throws IOException {

        String jsonString = "{\"context\":{\"class\":\"uk.gov.gchq.palisade.Context\",\"contents\":{\"purpose\":\"testContext\"}},\"user\":{\"user_id\":\"testUserId\",\"attributes\":{}},\"resource\":{\"class\":\"uk.gov.gchq.palisade.resource.impl.FileResource\",\"id\":\"/test/file.format\",\"attributes\":{},\"connectionDetail\":{\"class\":\"uk.gov.gchq.palisade.service.SimpleConnectionDetail\",\"serviceName\":\"test-service\"},\"parent\":{\"class\":\"uk.gov.gchq.palisade.resource.impl.SystemResource\",\"id\":\"/test/\"},\"serialisedFormat\":\"format\",\"type\":\"java.lang.String\"}}";

        ObjectContent<PolicyRequest> policyRequestObjectContent =  jacksonTester.parse(jsonString);

        PolicyRequest policyRequest =  policyRequestObjectContent.getObject();
        assertThat(policyRequest.getContext().getPurpose()).isEqualTo("testContext");
        assertThat(policyRequest.getUser().userId).isEqualTo("testUserId");
        assertThat(policyRequest.getResource().getId()).isEqualTo("/test/file.format");

    }

}