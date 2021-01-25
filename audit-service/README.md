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

# <img src="../logos/logo.svg" width="180">

# Audit Service

The Audit service accepts incoming messages on the `error` and `success` Kafka topics, these messages
contain all the details of the initial request and any other relevant information. This information
will be passed to any local audit services that have been implemented. This service does not have any
output Kafka topics.

## Message model

| AuditSuccessMessage | AuditErrorMessage | 
|:--------------------|:------------------|
| *token              | *token            |
| userId              | userId            | 
| resourceId          | resourceId        | 
| context             | context           | 
| serviceName         | serviceName       | 
| timestamp           | timestamp         | 
| serverIP            | serverIP          | 
| serverHostname      | serverHostname    |
| attributes          | attributes        |
| leafResourceId      | error             |

*The token value come from the headers of the Kafka message that the service receives. This links the
audit message to the original request that was made.

If an error has occurred during a request/read then an AuditErrorMessage will be consumed from
the `error` Kafka topic. This type of message can be sent from any of the Palisade services 
(e.g user-service or policy-service).

If the request/read was successful then an AuditSuccessMessage will be consumed from the `success`
Kafka topic. This type of message can only be sent from either the `filtered-resource-service` or
the `data-service`.

## Kafka Interface

The application will not receive any `START` or `END` messages on either the `success` or `error` Kafka topics.
The `success` topic will only consist of AuditSuccessMessage objects and the `error` topic will only consist of 
AuditErrorMessage objects. The service will consume these messages and process them accordingly but there is no
output from this service, instead it will acknowledge the incoming message so that it does not get processed more than
once.

## Rest Interface

The application exposes one REST endpoint for the purpose of debugging:
* `POST /api/audit`
  - accepts an `x-request-token` `String` header, any number of extra headers.
  - accepts either an `AuditSuccessMessage` or an `AuditErrorMessage` as the request body
  - returns a `ResponseEntity` with the HTTP status.
  
## Example Error JSON Request
```
curl -X POST api/audit -H "content-type: application/json" --data \
{
   "userId":"originalUserID",
   "resourceId":"testResourceId",
   "context":{
      "class":"uk.gov.gchq.palisade.Context",
      "contents":{
         "purpose":"testContext"
      }
   },
   "serviceName":"testServicename",
   "timestamp":"2018-2021-11-26T10:28:54.118082Z",
   "serverIP":"testServerIP",
   "serverHostname":"testServerHostname",
   "attributes":{
      "messagesSent":"23"
   },
   "error":{
      "cause":null,
      "stackTrace":[...],
      "localizedMessage":"Something went wrong!"
   }
}
```

## Example Success JSON Request
```
curl -X POST api/audit -H "content-type: application/json" --data \
{
   "userId":"originalUserID",
   "resourceId":"testResourceId",
   "context":{
      "class":"uk.gov.gchq.palisade.Context",
      "contents":{
         "purpose":"testContext"
      }
   },
   "serviceName":"testServicename",
   "timestamp":"2018-2021-11-26T10:32:28.818842Z",
   "serverIP":"testServerIP",
   "serverHostname":"testServerHostname",
   "attributes":{
      "messagesSent":"23"
   },
   "leafResourceId":"testLeafResourceId"
}
```