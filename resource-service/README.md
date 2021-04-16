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

# Resource Service

The Resource service accepts an incoming message from the `user` Kafka topic which contains the resourceId that is being accessed 
(this could be an actual file, or a directory that could contain many files and/or sub-directories). The service will then query the backing store to
see if the requested resourceId has been stored. If this is not the case then the request will be passed onto the local implementation of 
the Resource service. All the returned resources will be within an Akka stream, each element in the stream is then consumed and
added to the `resource` Kafka topic to be processed by the Policy service.

## Message Model and Database Domain

| ResourceRequest | ResourceResponse | AuditErrorMessage | 
|:----------------|:-----------------|:------------------|
| userId          | userId           | *token            | 
| resourceId      | resourceId       | userId            |  
| context         | context          | resourceId        |
| user            | user             | context           | 
|                 | resource         | exception         | 
|                 |                  | serverMetadata    | 

(fields marked with * are acquired from headers metadata)

The service accepts a `ResourceRequest` from the User service, finds all the resources associated with the resourceId as a stream, the stream is 
then consumed and for each resource a `ResourceResponse` is created and then sent to the Policy service for further processing.

## Kafka Interface

The application receives 3 messages for each token, a `START` message, a message containing a `ResourceRequest` and an `END` message. The `START` gets
consumed by the service, it is then acknowledged as the start of the resources and is then written to the `resource` Kafka topic. 
The `ResourceRequest` message then gets consumed by the service and for each resource a `ResourceResponse` object is created. This then gets written 
to the `resource` Kafka topic. Once all the`ResourceResponse`s have been written to the topic, the `END` message gets written to the `resource` topic to 
mark the end of the resources for this request. If any errors are thrown within the service, the original request, along with the thrown exception are captured in an
`AuditErrorMessage` and written to the Kafka `error` topic.

## REST Interface

The application exposes one REST endpoint for the purpose of debugging:

* `POST /api/resource`
    - accepts an `x-request-token` `String` header, any number of extra headers, and a `ResourceRequest` body
    - returns a `ResourceResponse` for each resource found. This response contains the userId, resourceId, context, user and resource.

## Example JSON Request

```
curl -X POST api/resource -H "content-type: application/json" --data \
{
   "userId":"test-user-id",
   "resourceId":"file:/test/resourceId/",
   "context":{
      "class":"uk.gov.gchq.palisade.Context",
      "contents":{
         "purpose":"test-purpose"
      }
   },
   "user":{
      "userId":{
         "id":"test-user-id"
      },
      "roles":[
         "role"
      ],
      "auths":[
         "auth"
      ],
      "class":"uk.gov.gchq.palisade.User"
   }
}
```

## Example JSON Response

```
{
   "userId":"test-user-id",
   "resourceId":"file:/test/resourceId/",
   "context":{
      "class":"uk.gov.gchq.palisade.Context",
      "contents":{
         "purpose":"test-purpose"
      }
   },
   "user":{
      "userId":{
         "id":"test-user-id"
      },
      "roles":[
         "role"
      ],
      "auths":[
         "auth"
      ],
      "class":"uk.gov.gchq.palisade.User"
   },
   "resource":{
      "class":"uk.gov.gchq.palisade.resource.impl.FileResource",
      "id":"file:/test/resourceId/data1.txt",
      "attributes":{
         
      },
      "connectionDetail":{
         "class":"uk.gov.gchq.palisade.service.SimpleConnectionDetail",
         "serviceName":"data-service"
      },
      "parent":{
         "class":"uk.gov.gchq.palisade.resource.impl.DirectoryResource",
         "id":"file:/test/resourceId/",
         "parent":null
      },
      "serialisedFormat":"txt",
      "type":"type"
   }
}
```

## Uploading resources to the backing store on service start-up

It may be that some example resources may need to be added to the backing store before, for example, a test run of the Palisade system gets performed. This is solved by 
using Spring to upload resource(s) to the service from a yaml file. An example of this can be seen in this
[test-resource.yaml](src/contract-tests/resources/application-test-resource.yaml) file which adds the resource information to the backing store when the service starts up.

## Hadoop and Windows

Windows users will have problems with Hadoop integration testing. Included [here](src/component-tests/resources/hadoop-3.2.1/bin) is a Windows-compatible set of hadoop binaries.

To 'install' on Windows, an additional step is required - copy the [hadoop.dll](src/component-tests/resources/hadoop-3.2.1/bin/hadoop.dll) to `C:\Windows\System32`. This should enable
the `HadoopResourceServiceTest` to run correctly.

## Database Entities and Structuring

```
                            === CompletenessEntity ====
                            |* entity-id : String     |
             ---------------|* entity-type : Enum     |---------------
             |              ---------------------------              |
             |                           |                           |
             V                           V                           V
=== ResourceEntity ======== === TypeEntity ============ === SerialisedFmtEntity ===
|* resource-id: String    | |* type: String           | |* serialised-fmt: String |
|* parent-id: String      | | resource-id: String     | |  resource-id: String    |
|  resource: JSON         | --------------------------- ---------------------------
---------------------------
```

(`*` marks an indexable field)

### Motivation

To ease load on an external resource-service provider, a cache-like storage mechanism is used to return data for previously-queried resources. The code for this can be found in
the [StreamingResourceServiceProxy](src/main/java/uk/gov/gchq/palisade/service/resource/service/StreamingResourceServiceProxy.java) and [JpaPersistenceLayer](src/main/java/uk/gov/gchq/palisade/service/resource/repository/JpaPersistenceLayer.java).

Due to the tree-structure of filesystems, native caching of requests-responses results in a large amount of data while not providing any "smart" methods to the caching mechanism. For example, requesting `/some/directory/with-lots-of-files` may return (and
cache) 1000 resources, but requesting `/some/directory` will result in a cache miss, as would `/some/directory/with-lots-of-files/big-subdirectory`. Instead, a persistence store is used to break resource trees apart into their individual nodes and store
them separately, reconstructing parents and tree structure when requested again.

### Implementation

The persistence store is split into a number of separate repositories, one for each queryable index on a resource (i.e. resource-id, type, format). These indexable fields are referred to in code through an EntityType enum. These each store only "complete"
sets of information - directories for which have been queried in their entirety, either by directly requesting it, or requesting one of its parents. A separate repository stores pairs of resource-ids and entity-types.

### Example

A request to the resource-service may then look like:

* Get resources by some indexable field (or member of the [`EntityType` enum](src/main/java/uk/gov/gchq/palisade/service/resource/domain/EntityType.java) enum, say `resource-id`)
* Check the [Completeness Repository](src/main/java/uk/gov/gchq/palisade/service/resource/repository/CompletenessRepository.java) for whether the "cache" can return a "complete" set of information
    * If the `entity-type`-`resource-id` pair is found, continue to return from our "cache", otherwise return from the real resource-service
* Get by the indexable field in the appropriate repository
    * [Type Repository](src/main/java/uk/gov/gchq/palisade/service/resource/repository/TypeRepository.java) and [Serialised Format Repository](src/main/java/uk/gov/gchq/palisade/service/resource/repository/SerialisedFormatRepository.java) are not tree-like
      and return a collection of leaf-resource-ids to directly get-and-return
    * [Resource Repository](src/main/java/uk/gov/gchq/palisade/service/resource/repository/ResourceRepository.java) is tree-like, so we need to recursively get the child resources
        * Query the repository recursively for a collection of resources with a parent matching our current node
* Re-assemble the parents of all our leaf-resources
    * Query the [Resource Repository](src/main/java/uk/gov/gchq/palisade/service/resource/repository/ResourceRepository.java) for resources matching our node's parent-id
* Return the completed result

This can all be achieved with a Java Streams pipeline, allowing for reasonable latency and parallelization.

## License

Palisade-Services is licensed under the [Apache 2.0 License](https://www.apache.org/licenses/LICENSE-2.0) and is covered
by [Crown Copyright](https://www.nationalarchives.gov.uk/information-management/re-using-public-sector-information/copyright-and-re-use/crown-copyright/).
