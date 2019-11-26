<!---
Copyright 2019 Crown Copyright

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

# <img src="logos/logo.svg" width="180">

### Scalable Data Access Policy Management and Enforcement

#### Project Build

This is a classic multi-module maven java project with a Spring Boot Parent. Each module defines an individual service endpoint within Palisade
(with the exception of the service-launcher module). Of those services all of them are configured to build a docker container embedded with the
shaded jar and install it into the configured registry or cache (with the exception of the discovery-service).

```mvn clean install```

To build an individual service:

```mvn clean install -pl <module name>```

Having completed updates to an individual service forward deployment into the cluster can be triggered by updating the image tag which is composed
of a combination of the release tag and the git hash (of the local repo). This is achieved via profile activation (**pi**, "push image"). Eg:

```mvn clean install -pl <module name> -P pi```

#### Helm installation

It is recommended that Palisade is released to Kubernetes via helm using the templates included 
within this project. As a prerequisite the helm client will need to be installed on the client 
and the Kubernetes context configured to point at the target cluster.

```kubectl config get-contexts```

```kubectl config use-context <name>```

All deployment parameters are defined in the root ```values.yaml``` file, see inline comments for details.

Example first deployment to a local cluster (from the project root directory) :

```helm upgrade --install palisade . --set traefik.install=true```

This will deploy the traefik ingress controller and install Palisade with a deployment name of "palisade" into the defult namespace
so that the application will be available at ```http://localhost/palisade``` and the traefik dashboard will be available at 
```http://localhost:8080/dashboard/#/```.

Multiple instances of Palisade may be deployed on the same cluster, separated by namespace. The ingress controller will be configured
to provide a route via that namespace.

```helm upgrade --install test . --namespace testing```

This will deploy an additional instance of Palisade called test which may be accessed at ```http://localhost/testing/palisade```

#### Helm upgrade

Helm will only deploy artifacts to the cluster on upgrade that are new or changed. Pods that require re-deployment must advance the
image tag using the "push image" profile during the build, as shown above.

#### Helm delete

Custom resource definition (CRD) files required by the traefik configurations are pre-loaded by helm and as such must be removed manually if
the ingress controller sub-chart is removed or subsequent installations will fail. To achieve this run:

```kubectl delete -f charts/traefik/templates/custom-resource.yaml```