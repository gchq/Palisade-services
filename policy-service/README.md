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
# Policy Service

<span style="color:red">**Note:** As noted in the [documentation root](../README.md) Palisade is in an early stage of development, therefore the precise APIs contained in these service modules are likely to adapt as the project matures.</span>

## Overview

The core API for the policy service.

The responsibilities of the policy service is to provide the set of rules
(filters or transformations) that need to be applied to each resource that
has been requested, based the user and context.

**Note:** A resource could be a file, stream, directory or even the system
resource (policies added to the system resource would be applied globally).
