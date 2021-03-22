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

The Audit service accepts incoming messages on the `error` and `success` Kafka topics, these messages contain all the details of the initial request and any other relevant information. 
This information will be passed to any local audit services that have been implemented. This service does not have any output Kafka topics.

## High level architecture

<!--- 
See audit-service/doc/audit-service.drawio for the source of this diagram
--->
![Audit Service diagram](doc/audit-service.png)

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

*The token value come from the headers of the Kafka message that the service receives. This links the audit message to the original request that was made.

If an error has occurred at any stage during either the request or read phases then an AuditErrorMessage will be added to the `error` Kafka topic by the service that encountered the issue. 
This type of message can be sent from any of the Palisade services (e.g. User Service or Policy Service).
This message will then be read by the Audit Service and passed onto the local Audit Service implementation to allow the details of the error to be logged.

If the message on the `error` topic cannot be deserialised by the Audit Service then a file, containing the message, will be created and added to the local file system.
This value is configured within the application yaml files and can be set to different values depending on the profile that is used when starting the Audit Service.
Any files that are created will have the same template for the file name, `Error-Timestamp`.

If the request or read was successful then an AuditSuccessMessage will be added to the `success` Kafka topic by the service that created the success message. 
This type of message can only be sent from either the Filtered Resource Service (the end of the request phase), or the Data Service (the end of the read phase).
This message will then be read by the Audit Service and passed onto the local Audit Service implementation to allow the details of the error to be logged.

If the message on the `success` topic cannot be deserialised by the Audit Service then a file, containing the message, will be created and added to the local file system.
This directory value is configured within the application yaml files and can be set to different values depending on the profile that is used when starting the Audit Service.
Any files that are created will have the same template for the file name, `Success-Timestamp`.

## Kafka Interface

The application will not receive any `START` or `END` messages on either the `success` or `error` Kafka topics. The `success` topic will only consist of AuditSuccessMessage objects and the `error` topic will only consist of AuditErrorMessage objects. The
service will consume these messages and process them accordingly but there is no output from this service, instead it will acknowledge the incoming message so that it does not get processed more than once.

## Rest Interface

The application exposes one REST endpoint for the purpose of debugging:

* `POST /api/audit`
    - accepts an `x-request-token` `String` header, any number of extra headers.
    - accepts either an `AuditSuccessMessage` or an `AuditErrorMessage` as the request body
    - returns a `ResponseEntity` with the HTTP status.

## Example AuditErrorMessage JSON Request

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
   "timestamp":"2020-11-26T10:28:54.118082Z",
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

## Example AuditSuccessMessage JSON Request

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
   "timestamp":"2020-11-26T10:32:28.818842Z",
   "serverIP":"testServerIP",
   "serverHostname":"testServerHostname",
   "attributes":{
      "messagesSent":"23"
   },
   "leafResourceId":"testLeafResourceId"
}
```

## License

Palisade-Services is licensed under the [Apache 2.0 License](https://www.apache.org/licenses/LICENSE-2.0) and is covered by [Crown Copyright](https://www.nationalarchives.gov.uk/information-management/re-using-public-sector-information/copyright-and-re-use/crown-copyright/).
