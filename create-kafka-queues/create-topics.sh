#!/bin/bash
# Copyright 2020 Crown Copyright
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
# Uses Busybox shell commands
# busybox sh is based on ash that implements a subset of the
# POSIX specification of sh (in the POSIX locale, it is compliant for the most
# part) with very few extensions, and in particular, not this one.
# This is for compatability reasons, N.B some of the kafka client commands
# make use of bash.

#write_to_kafka(NAME PARTITION REPLICATIONFACTOR)
#NAME: The name of the topic to create
#PARTITION: The number of partitions to associate with this topic
#REPLICATION: The replication-factor to associate with this topic
#e.g write_to_kafka palisade 1 1
write_to_kafka () {
  read -r NAME <<< $1
  read -r PARTITION <<< $2
  read -r REPLICATION <<< $3
  echo ./bin/kafka-topics.sh --create --replication-factor $REPLICATION --partitions $PARTITION --zookeeper $ZOOKEEPER --topic $NAME
  until ./bin/kafka-topics.sh --create --replication-factor $REPLICATION --partitions $PARTITION --zookeeper $ZOOKEEPER --topic $NAME; do
    echo Retrying creation of topic $NAME $PARTITION $REPLICATION
    sleep 10
  done
}

if [ $# -eq 1 ]; then
  echo "1 argument passed, assumed that this is a docker test"
  # if `docker run` only has one arguments, we assume user is running alternate command like `bash` to inspect the image
  exec "$@"
else
  printenv

  if [ -z ${ZOOKEEPER+palisade-zookeeper:2181} ]; then
    ZOOKEEPER="palisade-zookeeper:2181"
  fi

  until ./bin/kafka-topics.sh --zookeeper $ZOOKEEPER --list; do
    echo "Waiting for zookeeper, retrying in 20 seconds"
    sleep 20
  done

  #Search for all environmental variables starting with the word: TOPIC
  for topic in "${!TOPIC@}"; do
    # Use variable indirection to get the contents of TOPICX e.g palisade 1 1
    write_to_kafka ${!topic}
  done
  echo "topics created as follows - "
  ./bin/kafka-topics.sh --zookeeper $ZOOKEEPER --list
fi
