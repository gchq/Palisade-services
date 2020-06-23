package uk.gov.gchq.palisade.service.user.response;

import org.junit.jupiter.api.Test;
import uk.gov.gchq.palisade.service.user.response.common.domain.User;

import static org.junit.jupiter.api.Assertions.*;

class UserResponseTest {

    /**
     * Test that the object can be constructed using the information in a UserRequest object.
     */
    @Test
    void constructorTest() {
      //  private final String token; // Unique identifier for this specific request end-to-end
       // private final User user;  //Representation of the User
      //  private final String resourceId;  //Resource that that is being asked to access
      //  private final String context;  // represents the context information as a Json string of a Map<String, String>


        String token ="token";
        User user = User.create("userID");
        String resourceId ="resourceId";
        String context ="context";

        UserResponse respopnse = UserResponse.Builder.create()
                .withToken(token)
                .withUser(user)
                .withResourceId(resourceId)
                .withContext(context);


        /*
       final AuditRequest auditRequest = AuditRequest.RegisterRequestExceptionAuditRequest.create(requestId)
                .withUserId(userId)
                .withResourceId(resource.getId())
                .withContext(context)
                .withException(exception)
                .withServiceClass(Service.class);

        * */

    }



}