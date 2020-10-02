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

# Palisade Attribute-Masking-Service

The attribute-masking-service is the final transformation the palisade system applies to resources before they are returned.
The service performs two functions:
* Store the full details of the authorised request in a persistence store, to be later retrieved by the data-service
* Mask the leafResource, removing any sensitive information


## Message Model and Database Domain

| AttributeMaskingRequest | AttributeMaskingResponse | AuditErrorMessage | AuthorisedRequestEntity
|:------------------------|:-------------------------|:------------------|:-----------------------
| *token                  | *token                   | *token            | token
| userId                  | userId                   | userId            | uniqueId (token-leafResource.id)
| resourceId              | resourceId               | resourceId        | ---
| context                 | context                  | context           | context
| user                    | maskedLeafResource       | exception         | user
| leafResource            |                          | serverMetadata    | leafResource
| rules                   |                          |                   | rules
(fields marked with * are acquired from headers metadata)

The service takes in an `AttributeMaskingRequest` and a request `Token`.
This request is persisted in a store as an `AuthorisedRequestEntity`.
The `LeafResource` attached to the request is then masked, removing any excessive or sensitive metadata which a client may be prohibited from accessing.
The resulting masked `LeafResource` is then attached to an `AttributeMaskingResponse` and outputted from the service.

### Future Enhancements
The masking operation may in the future apply attribute-level `Rule`s using the `User` and `Context` for fine-grained control over client metadata access.


## REST Interface

The application exposes two REST endpoints:
* `POST /api/mask`
  - takes an `x-request-token` `String` header, any number of extra headers, and an `AttributeMaskingRequest` body
  - returns a `202 ACCEPTED` after writing the headers and body to kafka
  
* `POST /api/mask/multi`
  - takes an `x-request-token` `String` header, any number of extra headers, and a `List` of `AttributeMaskingRequest` body
  - returns a `202 ACCEPTED` after writing the headers and bodies to kafka


## Kafka Interface

The application takes messages from the upstream Kafka `rule` topic and reads them as `AttributeMaskingRequest`s.
These are then processed into `AttributeMaskingResponse`s and written to the downstream Kafka `masked-resource` topic.
The `x-request-token` is sent in the Kafka headers.
In case of errors, the original request and thrown exception are both captured in an `AuditErrorMessage` and written to the Kafka `error` topic.


## Example JSON Request
```
curl -X POST attribute-masking-service/api/mask -H "x-request-token: test-request-token" -H "content-type: application/json" --data \
'{
  "userId": "test-user-id",
  "resourceId": "/test/resourceId",
  "context": {
    "class": "uk.gov.gchq.palisade.Context",
    "contents": {
      "purpose": "test-purpose"
    }
  },
  "user": {
    "userId": {
      "id": "test-user-id"
    },
    "roles": [],
    "auths": [],
    "class": "uk.gov.gchq.palisade.User"
  },
  "resource": {
    "class": "uk.gov.gchq.palisade.resource.impl.FileResource",
    "id": "/test/resourceId",
    "attributes": {},
    "connectionDetail": {
      "class": "uk.gov.gchq.palisade.service.SimpleConnectionDetail",
      "serviceName": "test-data-service"
    },
    "parent": {
      "class": "uk.gov.gchq.palisade.resource.impl.SystemResource",
      "id": "/test/"
    },
    "serialisedFormat": "avro",
    "type": "uk.gov.gchq.palisade.test.TestType"
  },
  "rules": {
    "message": "no rules set",
    "rules": {}
  }
}'
```


## Example JSON Response
```
{
  "userId": "test-user-id",
  "resourceId": "/test/resourceId",
  "context": {
    "class": "uk.gov.gchq.palisade.Context",
    "contents": {
      "purpose": "test-purpose"
    }
  },
  "resource": {
    "class": "uk.gov.gchq.palisade.resource.impl.FileResource",
    "id": "/test/resourceId",
    "attributes": {},
    "connectionDetail": {
      "class": "uk.gov.gchq.palisade.service.SimpleConnectionDetail",
      "serviceName": "test-data-service"
    },
    "parent": {
      "class": "uk.gov.gchq.palisade.resource.impl.SystemResource",
      "id": "/test/"
    },
    "serialisedFormat": "avro",
    "type": "uk.gov.gchq.palisade.test.TestType"
  }
}
```


## License

Palisade-Services is licensed under the [Apache 2.0 License](https://www.apache.org/licenses/LICENSE-2.0) and is covered by [Crown Copyright](https://www.nationalarchives.gov.uk/information-management/re-using-public-sector-information/copyright-and-re-use/crown-copyright/).
