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

The core API for the data service. It accepts a request from the client and returns the resources that have been processed from the initial request for resources.  These resources will be filtered and redacted based on the processing of the request in the inital resource request.

The client request will contain the token that was returned from the initial resource request for uniquely identifying this request and the resource id that identifies this specific part of this request.


## Message Model and Database Domain



| Client Resource request to               | Client Request to 
| palisade-service for access to resources | data-services for prepared resources                                        
| PalisadeRequest | PalisadeClientResponse | DataRequest | Response                   AuditSuccessMessage | AuditErrorMessage | 
|:----------------|:-----------------------|:-----------------------------------------|:--------------------|:------------------|
| userId          | token                  | token       | **StreamingResponseBody    | *token              | *token            |
| resourceId      |                        |             |                            | userId              | *token            |
| context         |                        | resourceId  |                            | resourceId             | userId            |  
| context         |                        |             |                            | context                | resourceId        |
|                 |                        |             |                            | leafResourceId                                           |                                               context           | 
|                 |                        |             |                            | ***attributes                                                    exception         | 
|                 |                        |             |                            |                      serverMetadata    | 

(* token is contained in the header metadata for the AuditSuccessMessage and AuditErrorMessage)
(** holds an OutputStream of the data)
(** holds amoung other things, the number of records processed and the number of records returned)

