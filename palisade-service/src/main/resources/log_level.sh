#!/usr/bin/env bash
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

# Usage info
show_help() {
cat << EOF
Usage: ${0##*/} [-htdiew] [-l LEVEL] [-p PACKAGE]...
Modify the log level in every application pod in the same application release instance as enclosing pod at runtime to the given LEVEL,
filtered to the given java PACKAGE.

  -h          display this help and exit
  -l LEVEL    modify the log level to the given LEVEL (default TRACE)
  -tdiew      modify the log level to TRACE,DEBUG,INFO,ERROR,WARN only one of should be supplied
  -p PACKAGE  java package level to be modified (default uk.gov.gchq.palisade)

EOF
}

OPTIND=1

# Variables
LEVEL="TRACE"
PACKAGE="uk.gov.gchq.palisade"

while getopts "hl:p:tdiew" opt; do
  case $opt in
  h)
    show_help
    exit 0
    ;;
  l) LEVEL=${OPTARG}
     ;;
  p) PACKAGE=${OPTARG}
     ;;
  t) LEVEL="TRACE"
     ;;
  d) LEVEL="DEBUG"
     ;;
  i) LEVEL="INFO"
     ;;
  e) LEVEL="ERROR"
     ;;
  w) LEVEL="WARN"
     ;;
  *) show_help >&2
     echo "$opt"
     exit 1
     ;;
  esac
done

shift $((OPTIND-1))

# Point to the internal API server hostname
APISERVER=https://kubernetes.default.svc

# Path to ServiceAccount token
SERVICEACCOUNT=/var/run/secrets/kubernetes.io/serviceaccount

# Read this Pod's namespace
NAMESPACE=$(cat ${SERVICEACCOUNT}/namespace)

# Read the ServiceAccount bearer token
TOKEN=$(cat ${SERVICEACCOUNT}/token)

# Reference the internal certificate authority (CA)
CACERT=${SERVICEACCOUNT}/ca.crt

# Explore the API with TOKEN
export PYTHONIOENCODING=utf8

# Determine the release name of the current pod from the meta
read -r -d '' CMD1 << '--END1'
import sys, json;
ele=json.load(sys.stdin);
print (ele['metadata']['labels']['app.kubernetes.io/instance']);
--END1

export RELEASE=$(curl -s --cacert ${CACERT} --header "Authorization: Bearer ${TOKEN}" -X GET ${APISERVER}/api/v1/namespaces/"${NAMESPACE}"/pods/"${HOSTNAME}" | \
  python2 -c "$CMD1")

# Get a list of pods to process
read -r -d '' CMD2 << '--END2'
import sys, os, json;
ele=json.load(sys.stdin);
pods=ele['items']
release=os.environ["RELEASE"]
selected=list();
for pod in pods:
  if 'labels' in pod['metadata']:
    if 'app.kubernetes.io/instance' in pod['metadata']['labels']:
      if release==pod['metadata']['labels']['app.kubernetes.io/instance'] and "Job"!=pod['metadata']['ownerReferences'][0]['kind']:
        selected.append(pod['status']['podIP'] + ":" + str(pod['spec']['containers'][0]['livenessProbe']['httpGet']['port']));
print " ".join(selected)
--END2

PODS=$(curl -s --cacert ${CACERT} --header "Authorization: Bearer ${TOKEN}" -X GET ${APISERVER}/api/v1/namespaces/"${NAMESPACE}"/pods | \
  python2 -c "$CMD2")

echo "Changing log level to ${LEVEL} for release [${RELEASE}]"
for i in ${PODS}
do
  echo "Processing Pod ${i}"
  curl -i -X POST -H 'Content-Type: application/json' -d "{\"configuredLevel\": \"${LEVEL}\"}" "${i}"/actuator/loggers/"${PACKAGE}"
done