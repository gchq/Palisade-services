# Copyright 2019 Crown Copyright
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
{{- if eq .Values.global.deployment "codeRelease" }}
{{- if eq .Values.global.hosting "local" }}
{{- $path := index .Values "palisade-service" "image" "codeRelease" | lower | replace "." "-" | trunc 63 | trimSuffix "-" }}
{{- printf "%s/%s" .Values.global.persistence.classpathJars.local.hostPath $path }}
{{- else if eq .Values.global.hosting "aws" }}
{{- $path := index .Values "palisade-service" "image" "codeRelease" | lower | replace "." "-" | trunc 63 | trimSuffix "-" }}
{{- printf "%s/%s" .Values.global.persistence.classpathJars.aws.volumePath $path }}
{{- end }}
{{- else }}
{{- if eq .Values.global.hosting "local" }}
{{- $path := .Values.global.deployment | lower | replace "." "-" | trunc 63 | trimSuffix "-" }}
{{- printf "%s/%s" .Values.global.persistence.classpathJars.local.hostPath $path }}
{{- else if eq .Values.global.hosting "aws" }}
{{- $path := .Values.global.deployment | lower | replace "." "-" | trunc 63 | trimSuffix "-" }}
{{- printf "%s/%s" .Values.global.persistence.classpathJars.aws.volumePath $path }}
{{- end }}
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

