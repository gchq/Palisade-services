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
{{- define "data-service.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "data-service.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "data-service.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Common labels
*/}}
{{- define "data-service.labels" -}}
app.kubernetes.io/name: {{ include "data-service.name" . }}
helm.sh/chart: {{ include "data-service.chart" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{/*
Return the appropriate apiVersion for deployment.
*/}}
{{- define "deployment.apiVersion" -}}
{{- if semverCompare ">=1.9-0" .Capabilities.KubeVersion.GitVersion -}}
{{- print "apps/v1" -}}
{{- else -}}
{{- print "extensions/v1beta1" -}}
{{- end -}}
{{- end -}}

{{/*
Determine ingress root url
*/}}
{{- define "data-service.root" -}}
{{- $ns := include "palisade.namespace" . -}}
{{- if eq "default" $ns -}}
{{- printf "" -}}
{{- else -}}
{{- printf "/%s" $ns -}}
{{- end -}}
{{- end -}}

{{/*
Calculate a storage path based on the code release artifact id or the supplied value of codeRelease
*/}}
{{- define "data-service.deployment.path" }}
{{- if eq .Values.global.deployment "codeRelease" }}
{{- $path := .Values.image.codeRelease | lower | replace "." "-" | trunc 63 | trimSuffix "-" }}
{{- printf "%s/%s/classpath/%s" .Values.global.persistence.classpathJars.mountPath .Chart.Name $path }}
{{- else }}
{{- $path := .Values.global.deployment | lower | replace "." "-" | trunc 63 | trimSuffix "-" }}
{{- printf "%s/%s/classpath/%s" .Values.global.persistence.classpathJars.mountPath .Chart.Name $path }}
{{- end }}
{{- end }}

{{/*
Calculate the service config location
*/}}
{{- define "data-service.config.path" }}
{{- printf "/usr/share/%s/config/" .Chart.Name }}
{{- end }}

{{/*
Calculate a storage path based on the code release artifact id or the supplied value of codeRelease
*/}}
{{- define "data-service.classpathJars.name" }}
{{- printf "%s-%s-%s" .Values.global.persistence.classpathJars.name (include "data-service.deployment.revision" .) (include "palisade.namespace" .) | replace "/" "-"}}
{{- end }}

{{/*
Calculate a storage path based on the code release artifact id or the supplied value of codeRelease
*/}}
{{- define "data-service.classpathJars.mounts" }}
{{- if eq .Values.global.hosting "local" }}
{{- printf "%s/%s" .Values.global.persistence.classpathJars.local.hostPath (include "data-service.deployment.revision" .) }}
{{- else if eq .Values.global.hosting "aws" }}
{{- printf "%s/%s" .Values.global.persistence.classpathJars.aws.volumePath (include "data-service.deployment.revision" .) }}
{{- end }}
{{- end }}

{{/*
Calculate a storage name based on the code release artifact id or the supplied value of codeRelease
*/}}
{{- define "data-service.deployment.revision" }}
{{- $revision := index .Values "image" "codeRelease" | lower | replace "." "-" | trunc 63 | trimSuffix "-" }}
{{- printf "%s/%s" .Values.global.deployment $revision }}
{{- end }}
