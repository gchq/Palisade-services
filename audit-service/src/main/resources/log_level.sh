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

export RELEASE=$(curl -s --cacert ${CACERT} --header "Authorization: Bearer ${TOKEN}" -X GET ${APISERVER}/api/v1/namespaces/"${NAMESPACE}"/pods/${HOSTNAME} | \
  python2 -c "$CMD1")

read -r -d '' CMD2 << '--END2'
import sys, os, json;
ele=json.load(sys.stdin);
pods=ele['items']
release=os.environ["RELEASE"]
selected=list();
for pod in pods:
        if release==pod['metadata']['labels']['app.kubernetes.io/instance']:
                selected.append(pod['metadata']['name']);
print ",".join(selected)
--END2

curl -s --cacert ${CACERT} --header "Authorization: Bearer ${TOKEN}" -X GET ${APISERVER}/api/v1/namespaces/"${NAMESPACE}"/pods | \
  python2 -c "$CMD2"