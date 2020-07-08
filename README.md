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

# <img src="logos/logo.svg" width="180">

## Scalable Data Access Policy Management and Enforcement

### Project Build

This is a classic multi-module maven java project with a Spring Boot Parent.
Each module defines an individual service endpoint within Palisade (except for the services-manager module).
Each module is configured to build a docker container embedded with the jar and to install it into the configured registry or cache (except for the discovery-service and services-manager modules).
```
mvn clean install
```

To build an individual service:
```
mvn clean install -pl <module name>
```

Having completed updates to an individual service, deployment into the cluster can be triggered by updating the image tag.
This tag is composed of a combination of the release tag and of the git hash (of the local repo).
This is achieved via profile activation (**pi**, "push image"), eg:
```
mvn clean install -pl <module name> -P pi
```


### Considerations for running under Windows

In order to mount local directories to the data service, ***Windows users*** may find it necessary to adjust their firewall settings or change the network category for the "vEthernet (DockerNAT)" card to private via PowerShell:
```
Set-NetConnectionProfile -InterfaceAlias "vEthernet (DockerNAT)" -NetworkCategory Private
```
This may be required to be repeated after docker updates or system reboots.

Further, ***Windows Subsystem for Linux (WSL) users*** should note that the directory root must be configured as /c/Users... and not /mnt/c/Users... as this is configured as a network mount.
To do this, create a file called /etc/wsl.conf under linux and add the following:
```
[automount]
root = /
options = "metadata"
```
Changes will require a reboot to take effect.


### Helm Install

It is recommended that Palisade is released to Kubernetes via helm using the templates included within this project.
As a prerequisite, the helm client will need to be installed on the client and the Kubernetes context will need to be configured to point at the target cluster.
```
kubectl config get-contexts
kubectl config use-context <name>
```

Example first deployment to a local cluster (from the project root directory):
```  
 helm upgrade --install palisade . \
  --set global.persistence.classpathJars.local.hostPath=$(pwd),global.persistence.dataStores.palisade-data-store.local.hostPath=$(pwd),global.persistence.kafka.local.hostPath=$(pwd),global.persistence.redisMaster.local.hostPath=$(pwd),global.persistence.redisSlave.local.hostPath=$(pwd),global.persistence.zookeeper.local.hostPath=$(pwd),traefik.install=true,kafka.install=true,redis.install=true,global.hosting=local,redis-cluster.install=false --timeout=200s
```
This will deploy the traefik ingress controller and install Palisade with a deployment name of "palisade" into the default namespace.
The application will be available at `http://localhost/palisade` and the traefik dashboard will be available at `http://localhost:8080/dashboard/#/`.
The working directory from `$(pwd)` will be used as the mount-point for the data-service, as well as for finding classpath-jars and for kafka/redis persistence.

Multiple instances of Palisade may be deployed on the same cluster, separated by namespace.
The ingress controller will be configured to provide a route via that namespace.
It is required that the namespace exists prior to the installation:
```
kubectl create namespace testing
helm upgrade --install test . --namespace testing
```

This will deploy an additional instance of Palisade called `test` which may be accessed at `http://localhost/testing/palisade`


### Helm Upgrade

Helm will only deploy artifacts to the cluster that, on upgrade, are new or changed.
Pods that require re-deployment must advance the image tag using the "push image" profile during the build, as shown above.


### Generated Deployment Names

It is possible to let helm generate the deployment name and let the chart create a new namespace for it, then deploy it there:
```
helm upgrade --install --generate-name . --set global.uniqueNamespace=true
```

Some more important arguments are as follows:

| Argument                                | Definition
|:----------------------------------------|:----------------------------------------
| --timeout                               | If the post-install create-kafka-queues job fails, increase the timeout, **default is 60s**, **recommendation is 200+s** 
| **Local Deployments**                   |
| global.persistence.**xxx**.hostPath     | The host directory to use as a mount point for internal volumes
| **AWS Deployments**                     |
| global.persistence.**xxx**.volumeHandle | The handle of an AWS EFS volume to use for mounting
| global.persistence.**xxx**.volumePath   | The EFS volume directory to use as a mount point for internal volumes
| **Optional Installs**                   |
| traefik.install                         | Install the traefik ingress controller, **default=false**
| metrics-server.install                  | Install the metrics-server to enable horizontal scaling, **default=false**
| dashboard.install                       | Install the kubernetes dashboard, **default=false**
| kafka.install                           | Install Kafka and Zookeeper, **default=true**
| redis.install                           | Install Redis, **default=true for local, false for aws**
| redis-cluster.install                   | Install Redis-cluster, **default=false for local, true for aws**

If the [Kubernetes dashboard](https://kubernetes.io/docs/tasks/access-application-cluster/web-ui-dashboard/) is required it must be installed separately as a prerequisite.
The `dashboard.install` switch installs ingress definitions into traefik for access at `https://localhost/kubernetes`.
Access to the dashboard should be by token, which can be obtained by running the following command against the cluster:
```
kubectl -n kube-system describe secrets \
  `kubectl -n kube-system get secrets | awk '/clusterrole-aggregation-controller/ {print $1}'` \
  | awk '/token:/ {print $2}'
```


### Changing Application Logging Level

All instances of the palisade-service pod contain a script that will identify all application pods in that release and change their log level at runtime:
```
kubectl exec -it palisade-service-7bb4d75d85-g8cgx -- bash /usr/share/palisade-service/log-level -t
```

Use the `-h` flag to see usage instructions.
