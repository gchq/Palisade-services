<!---
Copyright 2020 Crown Copyright

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
--->

# <img src="../logos/logo.svg" width="180">

# User Service

The User Service is responsible for providing the other Palisade components with knowledge of the users of the system. 
Note that we are avoiding having a separate notion of "Palisade" users that are distinct from the normal user accounts of the system. There is no such thing as a "Palisade" user.  
One of the purposes of this service is to allow Palisade to adopt whatever notion of *user* the host environment has. For example, this may from a central directory service such as LDAP, 
host operating system account or PKI based user authentication.
The User Service separates this concern from the rest of the system. Other components use this service's API to request user details. 
Some deployments may also allow Palisade to add users to the system, hence the presence of the `addUser()` method in the `UserService` interface.

## Message Model and Database Domain

| UserRequest     | UserResponse     | AuditErrorMessage | AuditableUserResponse |
|:----------------|:-----------------|:------------------|:----------------------|
| *token          | *token           | *token            | *token                | 
| userId          | userId           | userId            | UserResponse          |  
| resourceId      | resourceId       | resourceId        | AuditErrorMessage     |
| context         | context          | context           |                       |
|                 | User             | exception         |                       | 
|                 |                  | serverMetadata    |                       |
  
(fields marked with * are acquired from headers metadata)

The service accepts a `UserRequest` and a token, then checks that the User is in the cache; the technology of which is chosen in the User Services application.yaml and Spring profiles.
If the User exists in the cache, then it, alongside any attributes are retrieved, and used to create a `UserReponse` object which is then packaged into a `AuditableUserResponse` object with no errors, 
and then this is sent via kafka, to the Resource Service for further processing. 
If the User does not exist in the cache, then a `NoSuchUserIdException` is thrown within the Service, which is caught, and packaged in the `AuditableUserReponse`, alongside the original `UserRequest` and an `AuditErrorMessage`, 
this is then sent to the Audit Service and logged appropriatly.

## REST Interface

The application exposes two REST endpoints used for debugging or mocking kafka entrypoints:
* `POST api/user`
  - accepts an `x-request-token` `String` header, any number of extra headers, and an `UserRequest` body
  - returns a `202 ACCEPTED` after writing the headers and `AuditableUserResponse` to kafka

## Example JSON Request
```
curl -X POST user-service/api/user -H "x-request-token: test-request-token" -H "content-type: application/json" --data \
'{
   "userId": "test-user-id",
   "resourceId": "/test/resourceId",
   "context": {
     "class": "uk.gov.gchq.palisade.Context",
     "contents": {
       "purpose": "purpose"
     }
   }
 }'
```


## Example JSON Response
```
'{
  "userId": "test-user-id",
  "resourceId": "/test/resourceId",
  "context": {
    "class": "uk.gov.gchq.palisade.Context",
    "contents": {
      "purpose": "purpose"
    }
  },
  "user": {
    "userId": {
      "id": "test-user-id"
    },
    "roles": [
      "roles"
    ],
    "auths": [
      "auths"
    ],
    "class": "uk.gov.gchq.palisade.User"
  }
}'
```

### Notes

The API is reasonably simple at present, and it is likely that this interface will grow.
Specifically, we anticipate that `UserService` implementations will connect to an account provisioning service as explained above, which will let Palisade retrieve the details of users that it previously knows nothing about. 
That is, we do **not** expect that every user retrieved via `getUser` will have previously been added via a corresponding `addUser` call.

## License

Palisade-Services is licensed under the [Apache 2.0 License](https://www.apache.org/licenses/LICENSE-2.0) and is covered by [Crown Copyright](https://www.nationalarchives.gov.uk/information-management/re-using-public-sector-information/copyright-and-re-use/crown-copyright/).
