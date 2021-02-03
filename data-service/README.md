<!---
Copyright 2018-2021 Crown Copyright

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
# Data Service

<span style="color:red">**Note:** As noted in the [documentation root](../README.md) Palisade is in an early stage of development, therefore the precise APIs contained in these service modules are likely to adapt as the project matures.</span>

## Overview

The core API for the data service accepts a request from the client and returns the resources that have been processed from the initial request for resources.  These resources will be filtered and redacted based on the processing of the request in the inital resource request.

The client request will contain the token and resource id that is used to uniquely identify this resource request.


## Message Model and Database Domain

| DataRequest     | (response to client)   | AuditSuccessMessage  | AuditErrorMessage |
|:----------------|:-----------------------|:---------------------|:------------------|
| token           | OutputStream           | *token               | *token            | 
| leafResourceId  |                        | leafResourceId       | leafResourceId    |  
|                 |                        | userId               | ***userId         |
|                 |                        | resourceId           | ***resourceId     |
|                 |                        | context              | ***context        | 
|                 |                        | **attributes         | ***attributes     |
|                 |                        | serverMetadata       | error             |
|                 |                        |                      | serverMetadata    |
  
*token is stored in the header metadata  
**attributes will include the numbers for records processed and records returned
***data that may not be available depending on when the error occurred

## REST Interface

The application exposes one endpoint for retrieving the resourses that were processed bu the  requestsitting on debugging or mocking kafka entrypoints:
* `POST api/user`
    - accepts an `x-request-token` `String` header, any number of extra headers, and a single `UserRequest` in the body
    - returns a `202 ACCEPTED` after writing the headers and `UserRequest` to kafka
* `POST api/user/multi`
    - accepts an `x-request-token` `String` header, any number of extra headers, and a list of `UserRequest`s within the body
    - returns a `202 ACCEPTED` after writing the headers and `UserRequest` to kafka

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
  