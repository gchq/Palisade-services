{{/*
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
# Copied from https://github.com/helm/charts/blob/6c85be7b88748171afd17affe8b1b57c66bf66a2/incubator/zookeeper/templates/NOTES.txt
*/}}

{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "zookeeper.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "zookeeper.fullname" -}}
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
Return the proper image name (for the init container volume-permissions image)
*/}}
{{- define "zookeeper.volumePermissions.image" -}}
{{- $registryName := .Values.volumePermissions.image.registry -}}
{{/*
Can be uncommented when we implent a imageRegistry in Values.global
{{- if .Values.global.imageRegistry }}
    {{- $registryName := .Values.global.imageRegistry -}}
{{- end -}}
*/}}
{{- printf "%s/%s:%s" $registryName .Values.volumePermissions.image.repository .Values.volumePermissions.image.tag | toString -}}
{{- end -}}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "zookeeper.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
The name of the zookeeper headless service.
*/}}
{{- define "zookeeper.headless" -}}
{{- printf "%s-headless" (include "zookeeper.fullname" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
The name of the zookeeper chroots job.
*/}}
{{- define "zookeeper.chroots" -}}
{{- printf "%s-chroots" (include "zookeeper.fullname" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}
