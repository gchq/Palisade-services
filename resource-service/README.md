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

# Palisade Resource-Service

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
To ease load on an external resource-service provider, a cache-like storage mechanism is used to return data for previously-queried resources.
The code for this can be found in the [StreamingResourceServiceProxy](src/main/java/uk/gov/gchq/palisade/service/resource/service/StreamingResourceServiceProxy.java) and [JpaPersistenceLayer](src/main/java/uk/gov/gchq/palisade/service/resource/repository/JpaPersistenceLayer.java).

Due to the tree-structure of filesystems, naïve caching of requests-responses results in a large amount of data while not providing any "smart" methods to the caching mechanism.
For example, requesting `/some/directory/with-lots-of-files` may return (and cache) 1000 resources, but requesting `/some/directory` will result in a cache miss, as would `/some/directory/with-lots-of-files/big-subdirectory`.
Instead, a persistence store is used to break resource trees apart into their individual nodes and store them separately, reconstructing parents and tree structure when requested again.

### Implementation
The persistence store is split into a number of separate repositories, one for each queryable index on a resource (i.e. resource-id, type, format).
These indexable fields are referred to in code through an EntityType enum.
These each store only "complete" sets of information - directories for which have been queried in their entirety, either by directly requesting it, or requesting one of its parents.
A separate repository stores pairs of resource-ids and entity-types.

### Example
A request to the resource-service may then look like:
* Get resources by some indexable field (or member of the [`EntityType` enum](src/main/java/uk/gov/gchq/palisade/service/resource/domain/EntityType.java) enum, say `resource-id`)
* Check the [Completeness Repository](src/main/java/uk/gov/gchq/palisade/service/resource/repository/CompletenessRepository.java) for whether the "cache" can return a "complete" set of information
    * If the `entity-type`-`resource-id` pair is found, continue to return from our "cache", otherwise return from the real resource-service
* Get by the indexable field in the appropriate repository
    * [Type Repository](src/main/java/uk/gov/gchq/palisade/service/resource/repository/TypeRepository.java) and [Serialised Format Repository](src/main/java/uk/gov/gchq/palisade/service/resource/repository/SerialisedFormatRepository.java) are not tree-like and return a collection of leaf-resource-ids to directly get-and-return
    * [Resource Repository](src/main/java/uk/gov/gchq/palisade/service/resource/repository/ResourceRepository.java) is tree-like, so we need to recursively get the child resources
        * Query the repository recursively for a collection of resources with a parent matching our current node
* Re-assemble the parents of all our leaf-resources
    * Query the [Resource Repository](src/main/java/uk/gov/gchq/palisade/service/resource/repository/ResourceRepository.java) for resources matching our node's parent-id
* Return the completed result

This can all be achieved with a Java Streams pipeline, allowing for reasonable latency and parallelization.



## License

Palisade-Services is licensed under the [Apache 2.0 License](https://www.apache.org/licenses/LICENSE-2.0) and is covered by [Crown Copyright](https://www.nationalarchives.gov.uk/information-management/re-using-public-sector-information/copyright-and-re-use/crown-copyright/).
