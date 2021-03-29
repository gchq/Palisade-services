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

The Data Service accepts client requests to retrieve resources that have been registered on their behalf. These will be
the response to the initial requests sent to the Palisade service that have been collected, filtered and possibly
redacted in conformance to the defined rules and to the context of the request. The client is expected to send a
request containing the token and resource id that is used to uniquely identify the resource request. The response will
be an output stream holding the data resources. To see more information on client requests, see the Palisade Client's
library.

The key components of the service is the implementation of the DataService interface and the supporting services.  This
comes in the form of an implementation of DataService interface, SimpleDataService, that uses the Palisade Reader
library for the implementation of a solution using a database. This is then wrapped in a class, AuditableDataService,
that will provide the data in a form that can be used in constructing the response that is sent back to the client, and
the audit message that is sent to the Audit Service using the AuditMessageService.


```java
interface DataService extends Service { {
  CompletableFuture<AuthorisedDataRequest> authoriseRequest(final DataRequest request);

  CompletableFuture<Boolean> read(final AuthorisedDataRequest request, final OutputStream out, final AtomicLong recordsProcessed, final AtomicLong recordsReturned);


}
```


# Flow of Control

![Data Service diagram](doc/data-service.png)


Data Service is implemented as a RESTFul Request-Response service using a Spring Controller as the service endpoint.  
The processing of each client request will drive the workflow though the service. This will start with 
authorising the request (`DataService#authoriseReqeust`). If successful, the next step will be to read the data
(`DataService#read`) which will return a stream of the data to the client. Upon completion, a audit message is sent to 
the Audit Service indicating a successful transfer of the data. If during the authorisation request or at any time in 
the read request there is a error, the request is stopped, and an error message is sent to the Audit Service.

Kafka streaming is only used for the processing of audit messages. This is exposed in the functionality provided by the
`AuditMessageService`.  This will take both audit success and error messages and process them using the functionality 
provided in the `AkkaRunnableGraph`'s `runner` method. In this method, the message types are separated into success and 
error messages where they are then used to construct messages for the respective kafka error and success topics.


## Message Model and Database Domain

| DataRequest     | (response to client)   | AuditSuccessMessage  | AuditErrorMessage |
|:----------------|:-----------------------|:---------------------|:------------------|
| *token          | OutputStream           | *token               | *token            | 
| leafResourceId  |                        | leafResourceId       | leafResourceId    |  
|                 |                        | userId               | ***userId         |
|                 |                        | resourceId           | ***resourceId     |
|                 |                        | context              | ***context        | 
|                 |                        | **attributes         | ***attributes     |
|                 |                        | serverMetadata       | error             |
|                 |                        |                      | serverMetadata    |
  
*token comes in the body of the request from the client (DataRequest) and is stored in the header metadata for the 
audit messages

**attributes will include the numbers for records processed and records returned

***data types that may not be available depending on when the error occurred


## REST Interface

The application exposes one endpoint to the client for retrieving the resources. This will be the data that has previously 
been requested and prepared in the initial request to Palisade services. 
* `POST data/read/chunked`
    - returns a `200 OK` and a streamed HTTP response body which will provides the resource.

## Example JSON Request
```
curl -X POST data-service/read/chunked  -H "content-type: application/json" --data \
'{
   "token": "test-token",
   "leafResourceId": "test-leafResourceId"
 }'
```
## Octet-Stream Response
The response body will be an octet-stream of data from the requested resource with policy rules applied, no matter the 
file type, for example, a user.json resource might be:
```
{
  usernamne: "alice",
  password: null,
  postcode: "SW1 XXX"
}
```
