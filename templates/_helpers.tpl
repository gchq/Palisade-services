# Copyright 2018-2021 Crown Copyright
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "palisade.name" }}
{{- default .Chart.Name .Values.fullnameOverride | lower | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "palisade.fullname" }}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.fullnameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Calculate a storage path based on the code release artifact id or the supplied value of codeRelease
*/}}
{{- define "palisade.deployment.path" }}
{{- if eq .Values.global.hosting "local" }}
{{- $path := index .Values "palisade-service" "image" "codeRelease" | lower | replace "." "-" | trunc 63 | trimSuffix "-" }}
{{- printf "%s/%s" .Values.global.persistence.classpathJars.local.hostPath $path }}
{{- else if eq .Values.global.hosting "aws" }}
{{- $path := index .Values "palisade-service" "image" "codeRelease" | lower | replace "." "-" | trunc 63 | trimSuffix "-" }}
{{- printf "%s/%s" .Values.global.persistence.classpathJars.aws.volumePath $path }}
{{- end }}
{{- end }}

{{/*
Calculate a storage name based on the code release artifact id or the supplied value of codeRelease
*/}}
{{- define "palisade.deployment.name" }}
{{- include "palisade.deployment.path" . | base }}
{{- end }}

{{/*
Calculate a storage full name based on the code release artifact id or the supplied value of codeRelease
*/}}
{{- define "palisade.deployment.fullname" }}
{{- .Values.global.persistence.classpathJars.name }}-{{- include "palisade.deployment.name" . }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "palisade.chart" }}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Modify the namespace if required
 */}}
{{- define "palisade.namespace" }}
{{- if $.Values.global.uniqueNamespace }}
{{- printf "%s" .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- if .Release.Namespace }}
{{- printf "%s" .Release.Namespace }}
{{- else }}
{{- printf "%s" $.Values.global.namespace | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "palisade.serviceAccountName" }}
{{- if .Values.serviceAccount.create }}
    {{- default (include "palisade.fullname" .) .Values.serviceAccount.name }}
{{- else }}
    {{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}

{{/*
Create the service name of the redis master
*/}}
{{- define "palisade.redis.fullname" -}}
{{- if .Values.global.redis.exports.fullnameOverride -}}
{{- .Values.global.redis.exports.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default "redis" .Values.global.redis.exports.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{/*
Create a list of headless redis-cluster nodes from service name and node count
*/}}
{{- define "palisade.redis-cluster.headlessNodes" -}}
{{- $count := (index .Values.global "redis-cluster").cluster.nodes | int -}}
{{- $clusterFullname := printf "%s-cluster" (include "palisade.redis-cluster.fullname" .) -}}
{{- $headlessPort := int (index .Values.global "redis-cluster").exports.redisPort -}}
{{- range $i, $v := until $count -}}
{{- printf "%s-%d.%s-headless:%d," $clusterFullname $i $clusterFullname $headlessPort -}}
{{- end -}}
{{- end -}}

{{/*
Create the service name of the redis cluster
*/}}
{{- define "palisade.redis-cluster.fullname" -}}
{{- if (index .Values.global "redis-cluster").exports.fullnameOverride -}}
{{- (index .Values.global "redis-cluster").exports.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default "redis-cluster" (index .Values.global "redis-cluster").exports.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{/*
Create the service name of kafka
*/}}
{{- define "palisade.kafka.fullname" -}}
{{- if .Values.global.kafka.exports.fullnameOverride -}}
{{- .Values.global.kafka.exports.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default "kafka" .Values.global.kafka.exports.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{/*
Create the service url (including port) of kafka
*/}}
{{- define "palisade.kafka.url" -}}
{{- $serviceName := (include "palisade.kafka.fullname" .) -}}
{{- $servicePort := default 9092 .Values.global.kafka.exports.port -}}
{{- printf "%s:%d" $serviceName ($servicePort | int) -}}
{{- end -}}

{{/*
Create the service name of zookeeper
*/}}
{{- define "palisade.zookeeper.fullname" -}}
{{- if .Values.global.zookeeper.exports.fullnameOverride -}}
{{- .Values.global.zookeeper.exports.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default "zookeeper" .Values.global.zookeeper.exports.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{/*
Create the service url (including port) of zookeeper
*/}}
{{- define "palisade.zookeeper.url" -}}
{{- $serviceName := (include "palisade.zookeeper.fullname" .) -}}
{{- $servicePort := default 2181 .Values.global.zookeeper.exports.port -}}
{{- printf "%s:%d" $serviceName ($servicePort | int) -}}
{{- end -}}
