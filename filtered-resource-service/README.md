<!---
Copyright 2021 Crown Copyright

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

# <img src="/logos/logo.svg" width="180">

# Filtered Resource Service

The Filtered-Resource Service sits at the end of the stream pipeline and has the task of returning resources (and their connection details) back to the client. Given a token `$[token]` from the Palisade Service, it is accessed by a Websocket request
to `ws://filtered-resource-service/resource/${token}`. The service will return each resource discovered by the Palisade system one-by-one to the client as requested, auditing that the client is now aware of these resources.

## High-Level Architecture

![Palisade diagram](doc/palisade-results.png)

The above diagram shows the decision-making architecture of the Attribute-Masking Service (left), Topic-Offset Service (middle) and Filtered-Resource Service (right). The service connects to three kafka topics as inputs - "masked-resource" from the
Attribute-Masking Service, "masked-resource-offset" from the Topic-Offset Service and "error" from all services that may produce errors. An additional kafka topic is used for auditing the client's access to each returned resource - this is the "success"
topic, which will be read later by the Audit Service.

For a given token, the offset (on the "masked-resource" topic) for this token is received from the "masked-resource-offset" and used to create a consumer starting from this point. This flow of messages is then filtered by their `X-Request-Token` header to
match the supplied token. Then, as each resource is requested over the websocket, the successful return of each resource to the client is audited to the Audit Service's "success" topic.

As can be seen in the diagram, the service's functions generally fall into one of four responsibilities:

* Handling incoming web requests, REST or WS (top-centre of diagram)
* Persisting and retrieving offsets for tokens (top-left of diagram, blue)
* Processing websocket requests and returning resources (bottom of diagram)
* Alerting the client to errors that occurred during processing (right of diagram, yellow)

## Breakdown of Service Components

<!---
See filtered-resource-service/doc/filtered-resource-service.drawio for the source for this diagram.
--->
![Filtered Resource Service diagram](doc/filtered-resource-service.png)

The main route of web requests - and subsequent websocket messages - through the service is shown in the above diagram. The green boxes are client web requests, purple are kafka topics and red is redis persistence.

### Token-Offset System

The top-half is the token-offset-system and its associated workers. This performs the task of persisting and retrieving offsets for tokens (top-left, blue of HLA diagram). We use an Akka Actor System to process both requests from the client and messages
from kafka. In the former case, the offset is either looked-up from redis or told to the actor from kafka. In the latter case, the offset is persisted to redis and then told to all running for that given token.

### Websocket Event Service

The lower-half is the websocket-event-service. An instance of this service is created for each open client websocket connection. The client and server communicate using the following protocol:

| Client Request | Server Response                | Notes
|:---------------|:-------------------------------|:----------------
| PING           | PONG                           | Application-level ping-pong liveliness check - note that this is different to [protocol-level PING/PONG frames](https://tools.ietf.org/html/rfc6455#section-5.5.2)
| CTS            | RESOURCE, ERROR, COMPLETE      | A client's clear-to-send message is met with either a RESOURCE from the server (id, type, format, connection-detail) or an ERROR (message-details). Once all RESOURCEs and ERRORs are exhausted, then a COMPLETE message is returned.

### Akka Http Server

The akka http-server plays a simple but key role in the service, providing both HTTP and WS endpoints for client connections (top-center of HLA diagram). It is not present in the above diagram as the decision flow is trivial (just a collection of
endpoints, most backed by existing Spring mechanisms). It exposes the following HTTP REST endpoints:

| Request Type   | Endpoint                       | Notes
|:---------------|:-------------------------------|:----------------
| GET            | `/health/$component`           | Provides an indicator for application health for a given component (can be empty), backed by Spring's health mechanism
| POST           | `/api/{resource,offset,error}` | Write a payload to an (upstream) kafka queue such that it will then be processed by the service
| GET, POST      | `/loggers/$package`            | Get or set logging level for a given package (can be empty), backed by Spring's logging mechanism

It also exposes the following Websocket endpoint:

| Request Type   | Endpoint                       | Notes
|:---------------|:-------------------------------|:----------------
| WS_UPGRADE     | `/resource/$token`             | Connect to the service and carry out the websocket protocol described above. There will be no indication from the server as to whether the provided token is valid until a RESOURCE, ERROR or COMPLETE message is received.

## Example Data Payloads

The Filtered-Resource Service requires a number of inputs from separate services to produce its results; as such, orchestrating a manual interaction with the service is quite verbose. The steps required are as follows:

1. POST onto the masked-resource queue (`http://filtered-resource-service/api/masked-resource`) as if an output has come from the Attribute-Masking Service
    * This includes an additional pair of empty `START` and `END` of stream messages before and after the data payload respectively
    * Additionally, all three of these messages (and all further messages) will include a `test-token` in the header - this is our token from the Palisade Service
1. POST onto the masked-resource-offset queue (`http://filtered-resource-service/api/masked-resource-offset`) the offset of our START marker
    * This doesn't have to be accurate, as long as it is less-than-or-equal-to the actual value - we will use `0` as our offset
1. Open a WS connection to `ws://filtered-resource-service/resource/$token` (this example will use [Hashrocket's ws tool](https://github.com/hashrocket/ws))
1. Send CTS messages from the client until the server responds with COMPLETE

### Auditing within the Filtered Resource Service
The Filtered Resource Service will send audit messages, specifically an `AuditErrorMessage` to the Audit Service via the error topic for the two following cases:
1. No start marker was observed before reading the resources. If resources are processed by the Filtered Resource Service before a start marker is observed, 
   it could indicate that there is an issue earlier on in the system which could cause the messages to fall out of ordered. In this case, an Audit Error Message is created, containing a `NoStartMarkerObserved` exception, 
   which is then sent to the Audit Service, and finally, the processing of the request is stopped. 
2. No Resources were contained in the request. If a start marker is observed, but no resources are contained in the request, the request could be invalid, for resources that aren't known to Paliasde, 
   or for resources that have been redacted. In this case, an Audit Error Message is created, containing a `NoResourcesObserved` exception, and is then sent to the Audit Service. 

## Message Model
| FilteredResourceRequest | WebSocketMessage | TopicOffsetMessage | AuditSuccessMessage | AuditErrorMessage |
|-------------------------|------------------|--------------------|---------------------|-------------------|
| *Token                  | *Token           | queuePointer       | *Token              | *Token            |
| userId                  | messageType      |                    | userId              | userId            |
| resourceId              | LeafResource     |                    | resourceId          | resourceId        |
| context                 |                  |                    | context             | context           |
| LeafResource            |                  |                    | serverMetadata      | exception         |
|                         |                  |                    | attributes          | serverMetadata    |
|                         |                  |                    |                     | attributes        |

### JSON REST Requests

1. Mimicking the Attribute-Masking Service:
   ```bash
   curl -X POST http://filtered-resource-service/api/masked-resource -H "X-Request-Token: test-request-token" -H "X-Stream-Marker: START"

   curl -X POST http://filtered-resource-service/api/masked-resource -H "X-Request-Token: test-request-token" -H "content-type: application/json" --data \
   '{
      "userId": "userId",
      "resourceId": "file:/file/",
      "context": {
         "class": "uk.gov.gchq.palisade.Context",
         "contents": {
            "purpose": "purpose"
         }
      },
      "resource": {
         "class": "uk.gov.gchq.palisade.resource.impl.FileResource",
         "id": "file:/file/resource.1",
         "attributes": {},
         "connectionDetail": {
            "class": "uk.gov.gchq.palisade.service.SimpleConnectionDetail",
            "serviceName": "data-service"
         },
         "parent": {
            "class": "uk.gov.gchq.palisade.resource.impl.SystemResource",
            "id": "file:/file/"
         },
         "serialisedFormat": "fmt",
         "type": "type"
      }
   }'

   curl -X POST http://filtered-resource-service/api/masked-resource -H "X-Request-Token: test-request-token" -H "X-Stream-Marker: END"
   ```

1. Mimicking the Topic-Offset Service:
   ```bash
   curl -X POST http://filtered-resource-service/api/masked-resource-offset -H "X-Request-Token: test-request-token" -H "content-type: application/json" --data '{"queuePointer":0}'
   ```

### JSON WS Interaction

Using a preferred WebSocket command-line tool (using [ws](https://github.com/hashrocket/ws), where `>` is sent from the client to the server, and `<` is received by the client from the server):

```bash
ws ws://filtered-resource-service/resource/test-token
> {"type":"CTS","headers":{},"body":null}
< {"type":"RESOURCE","headers":{"X-Request-Token":"test-token"},"body":{"class":"uk.gov.gchq.palisade.resource.impl.FileResource","id":"file:/file/resource.1","attributes":{},"connectionDetail":{"class":"uk.gov.gchq.palisade.service.SimpleConnectionDetail","serviceName":"data-service"},"parent":{"class":"uk.gov.gchq.palisade.resource.impl.SystemResource","id":"file:/file/"},"serialisedFormat":"fmt","type":"type"}}
> {"type":"CTS","headers":{},"body":null}
< {"type":"COMPLETE","headers":{"X-Request-Token":"test-token"}}
```
