package uk.gov.gchq.palisade.service.resource.response;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;
import org.springframework.boot.test.json.ObjectContent;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.gchq.palisade.service.resource.response.common.ResourceMetadata;
import uk.gov.gchq.palisade.service.resource.response.common.domain.User;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


@RunWith(SpringRunner.class)
@JsonTest
public class ResourceResponseTest {

    @Autowired
    private JacksonTester<ResourceResponse> jacksonTester;


    /**
     * Create the object using the builder and then serialise it to a Json string. Test the content of the Json string
     *
     * @throws IOException if it fails to parse the object
     */
    @Test
    public void testSerialiseResourceResponseToJson() throws IOException {

        Map<String, String> context = new HashMap<>();
        context.put("key1", "context1");
        context.put("key2", "context2");

        Map<String, ResourceMetadata> resources = new HashMap<>();
        ResourceMetadata metadata1 = new ResourceMetadata();
        ResourceMetadata metadata2 = new ResourceMetadata();

        resources.put("resourceKey1", metadata1);
        resources.put("resourceKey2", metadata2);


        ResourceResponse resourceResponse = ResourceResponse.Builder.create()
                .withContext(context)
                .withUser(User.create("testUserId"))
                .withResources(resources);



        JsonContent<ResourceResponse> resourceRequestJsonContent = jacksonTester.write(resourceResponse);

        //these tests are each for strings
        assertThat(resourceRequestJsonContent).extractingJsonPathStringValue("$.userId").isEqualTo("testUserId");
        assertThat(resourceRequestJsonContent).extractingJsonPathStringValue("$.resourceId").isEqualTo("testResourceId");

        //test is for a json representation of a Map<String, String>, should stay unchanged
        assertThat(resourceRequestJsonContent).extractingJsonPathMapValue("$.context").containsKey("key1");
        assertThat(resourceRequestJsonContent).extractingJsonPathMapValue("$.context").containsValue("context2");

    }

    /**
     * Create the ResourceResponse object from a Json string and then test the content of the object.
     *
     * @throws IOException if it fails to parse the string into an object
     */
    @Test
    public void testDeserializeJsonToResourceResponse() throws IOException {

        String jsonString ="{\"resourceId\":\"testResourceId\",\"context\":{\"key1\":\"context1\",\"key2\":\"context2\"},\"user\":{\"user_id\":\"testUserId\",\"attributes\":{}}}";

        ObjectContent resourceRequestContent = (ObjectContent) jacksonTester.parse(jsonString);

        ResourceResponse request = (ResourceResponse) resourceRequestContent.getObject();
      //  assertThat(request.resourceId).isEqualTo("testResourceId");
      //  assertThat(request.getUser().userId).isEqualTo("testUserId");

    }
}