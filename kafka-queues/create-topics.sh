#!/bin/bash
#
#
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

#Arg 1 is the TOPIC details in the format:
#NAME PARTITION REPLICATIONFACTOR
#e.g palisade 1 1
write_to_kafka () {
    IFS=' '
    read -r NAME PARTITION REPLICATION <<< $1
    until ./bin/kafka-topics.sh --create --replication-factor $REPLICATION  --partitions $PARTITION --zookeeper $ZOOKEEPER --topic $NAME; do
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
    IFS=' '

    if [ -n "$TOPIC1" ]; then
        echo $TOPIC1
        write_to_kafka $TOPIC1
    fi
    if [ -n "$TOPIC2" ]; then
        echo $TOPIC2
        write_to_kafka $TOPIC2
    fi
    if [ -n "$TOPIC3" ]; then
        echo $TOPIC3
        write_to_kafka $TOPIC3
    fi
    if [ -n "$TOPIC4" ]; then
        echo $TOPIC4
        write_to_kafka $TOPIC4
    fi
    if [ -n "$TOPIC5" ]; then
        echo $TOPIC5
        write_to_kafka $TOPIC5
    fi
    if [ -n "$TOPIC6" ]; then
        echo $TOPIC6
        write_to_kafka $TOPIC6
    fi
    if [ -n "$TOPIC7" ]; then
        echo $TOPIC7
        write_to_kafka $TOPIC7
    fi
    if [ -n "$TOPIC8" ]; then
        echo $TOPIC8
        write_to_kafka $TOPIC8
    fi
    ./bin/kafka-topics.sh --zookeeper $ZOOKEEPER --list
fi
