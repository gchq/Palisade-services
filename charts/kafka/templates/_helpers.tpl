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
# Copied from: https://github.com/helm/charts/blob/6c85be7b88748171afd17affe8b1b57c66bf66a2/incubator/kafka/templates/_helpers.tpl
*/}}

{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "kafka.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "kafka.fullname" -}}
{{- if .Values.exports.fullnameOverride -}}
{{- .Values.exports.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default .Chart.Name .Values.exports.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}

{{/*
Create a default fully qualified zookeeper name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
*/}}
{{- define "kafka.zookeeper.fullname" -}}
{{- if .Values.zookeeper.fullnameOverride -}}
{{- .Values.zookeeper.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default "zookeeper" .Values.zookeeper.nameOverride -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}

{{/*
Return the proper image name (for the init container volume-permissions image)
*/}}
{{- define "kafka.volumePermissions.image" -}}
{{- printf "%s/%s:%s" .Values.volumePermissions.image.registry .Values.volumePermissions.image.repository .Values.volumePermissions.image.tag | toString -}}
{{- end -}}

{{/*
Form the Zookeeper URL. If zookeeper is installed as part of this chart, use k8s service discovery,
else use user-provided URL
*/}}
{{- define "zookeeper.url" }}
{{- $port := .Values.zookeeper.port | toString }}
{{- if .Values.zookeeper.enabled -}}
{{- printf "%s:%s" (include "kafka.zookeeper.fullname" .) $port }}
{{- else -}}
{{- $zookeeperConnect := printf "%s:%s" .Values.zookeeper.url $port }}
{{- $zookeeperConnectOverride := index .Values "configurationOverrides" "zookeeper.connect" }}
{{- default $zookeeperConnect $zookeeperConnectOverride }}
{{- end -}}
{{- end -}}

{{/*
Derive offsets.topic.replication.factor in following priority order: configurationOverrides, replicas
*/}}
{{- define "kafka.replication.factor" }}
{{- $replicationFactorOverride := index .Values "configurationOverrides" "offsets.topic.replication.factor" }}
{{- default .Values.replicas $replicationFactorOverride }}
{{- end -}}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "kafka.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create unified labels for kafka components
*/}}

{{- define "kafka.common.matchLabels" -}}
app.kubernetes.io/name: {{ include "kafka.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- range $key, $value := .Values.podLabels }}
{{ $key }}: {{ $value }}
{{- end -}}
{{- end -}}

{{- define "kafka.common.metaLabels" -}}
helm.sh/chart: {{ include "kafka.chart" . }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{- define "kafka.broker.matchLabels" -}}
app.kubernetes.io/component: kafka-broker
{{ include "kafka.common.matchLabels" . }}
{{- end -}}

{{- define "kafka.broker.labels" -}}
{{ include "kafka.common.metaLabels" . }}
{{ include "kafka.broker.matchLabels" . }}
{{- end -}}

{{- define "kafka.config.matchLabels" -}}
app.kubernetes.io/component: kafka-config
{{ include "kafka.common.matchLabels" . }}
{{- end -}}

{{- define "kafka.config.labels" -}}
{{ include "kafka.common.metaLabels" . }}
{{ include "kafka.config.matchLabels" . }}
{{- end -}}

{{- define "kafka.monitor.matchLabels" -}}
app.kubernetes.io/component: kafka-monitor
{{ include "kafka.common.matchLabels" . }}
{{- end -}}

{{- define "kafka.monitor.labels" -}}
{{ include "kafka.common.metaLabels" . }}
{{ include "kafka.monitor.matchLabels" . }}
{{- end -}}

{{- define "serviceMonitor.namespace" -}}
{{- if .Values.prometheus.operator.serviceMonitor.releaseNamespace -}}
{{ .Release.Namespace }}
{{- else -}}
{{ .Values.prometheus.operator.serviceMonitor.namespace }}
{{- end -}}
{{- end -}}

{{- define "prometheusRule.namespace" -}}
{{- if .Values.prometheus.operator.prometheusRule.releaseNamespace -}}
{{ .Release.Namespace }}
{{- else -}}
{{ .Values.prometheus.operator.prometheusRule.namespace }}
{{- end -}}
{{- end -}}
