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

The Audit Service can have multiple implementations, configured in the [application.yaml](src/main/resources/application.yaml), 
but simply, all implementations receive, and log Audit Messages from the other Palisade services

The service accepts incoming messages on the `error` and `success` Kafka topics. These messages contain information about the original request and, depending on the message type, will contain either of the following:

1. In the case of an error message, all the details of the error. 
1. In the case of a success message, the id value of the returned resource.

This information will be passed to any local audit services that have been implemented. This service does not have any output Kafka topics.

## High level architecture

<!--- 
See audit-service/doc/audit-service.drawio for the source of this diagram
--->
![Audit Service diagram](doc/audit-service.png)

## Flow of Control

<!--- 
See audit-service/doc/audit-service-flow.drawio for the source of this diagram
--->
![Audit Service Flow diagram](doc/audit-service-flow.png)

* FRS (Filtered Resource Service)
* DS (Data Service)

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

If an error has occurred while Palisade is processing a client request then an AuditErrorMessage will be added to the `error` Kafka topic by the service that encountered the issue. 
This type of message can be sent from any of the Palisade services.
This message will be read by the Audit Service and passed to the service implementation to allow the details of the error message to be logged.

If the client request was successful then an AuditSuccessMessage will be added to the `success` Kafka topic by the service that created the success message.
This type of message can only be sent from either the Filtered Resource Service or the Data Service.
This message will be read by the Audit Service and passed to the service implementation to allow the details of the success message to be logged.

If an incoming message cannot be deserialised by the Audit Service then the received message will be added to a file that will be stored within a specified
directory. The directory is configured by adding the value to the [application.yaml](src/main/resources/application.yaml) configuration file under `audit.errorDirectory`.
This value can be changed depending on the profile that is used when the service is started.

The name of the file will contain the topic name the message was read from, either "Success" or "Error", and it will also include the timestamp of when the file was created.
This is done from within the [SerDesConfig](src/main/java/uk/gov/gchq/palisade/service/audit/stream/SerDesConfig.java) and if there are any issues creating or saving the file
then an error is logged in the service logs

## Kafka Interface

The application will not receive any `START` or `END` messages on either the `success` or `error` Kafka topics. The `success` topic will only consist of AuditSuccessMessage objects and the `error` topic will only consist of AuditErrorMessage objects. The
service will consume these messages and process them accordingly but there is no output from this service, instead it will acknowledge the incoming message so that it does not get processed more than once.
The messages from each topic are processed by the [RunnableGraph](src/main/java/uk/gov/gchq/palisade/service/audit/stream/config/AkkaRunnableGraph.java). This takes the deserialised message and passes it to the 
[Audit Service Proxy](src/main/java/uk/gov/gchq/palisade/service/audit/service/AuditServiceAsyncProxy.java). This service proxy will perform some checks on the received message:
* Check the type of message that was received, AuditErrorMessage or AuditSuccessMessage. A warning will be logged in the service if the message type is neither of these.
* If the type is an AuditSuccessMessage then the service will check to make sure that this has been sent by either the Filtered Resource Service or the Data Service

## Rest Interface

The Audit Service exposes two REST endpoints for the purpose of debugging:

* `POST /api/error`
    - accepts an `x-request-token` `String` header, any number of extra headers.
    - accepts an `AuditErrorMessage` as the request body
    - returns a `ResponseEntity` with the HTTP status `202 ACCETPED`.
* `POST /api/success`
  - accepts an `x-request-token` `String` header, any number of extra headers.
  - accepts an `AuditSuccessMessage` as the request body
  - returns a `ResponseEntity` with the HTTP status `202 ACCETPED`.

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
