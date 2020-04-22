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

    if [ -n "$TOPIC1" ]; then
        echo $TOPIC1
          until ./bin/kafka-topics.sh --create --replication-factor 1 --partitions 1 --zookeeper $ZOOKEEPER --topic $TOPIC1; do
            echo Retrying creation of topic $TOPIC1
            sleep 10
          done
    fi
    if [ -n "$TOPIC2" ]; then
        echo $TOPIC2
          until ./bin/kafka-topics.sh --create --replication-factor 1 --partitions 1 --zookeeper $ZOOKEEPER --topic $TOPIC2; do
            echo Retrying creation of topic $TOPIC1
            sleep 10
          done
    fi
    if [ -n "$TOPIC3" ]; then
        echo $TOPIC3
          until ./bin/kafka-topics.sh --create --replication-factor 1 --partitions 1 --zookeeper $ZOOKEEPER --topic $TOPIC3; do
            echo Retrying creation of topic $TOPIC3
            sleep 10
          done
    fi
    if [ -n "$TOPIC4" ]; then
        echo $TOPIC4
          until ./bin/kafka-topics.sh --create --replication-factor 1 --partitions 1 --zookeeper $ZOOKEEPER --topic $TOPIC4; do
            echo Retrying creation of topic $TOPIC4
            sleep 10
          done
    fi
    if [ -n "$TOPIC5" ]; then
        echo $TOPIC5
          until ./bin/kafka-topics.sh --create --replication-factor 1 --partitions 1 --zookeeper $ZOOKEEPER --topic $TOPIC5; do
            echo Retrying creation of topic $TOPIC5
            sleep 10
          done
    fi
    if [ -n "$TOPIC6" ]; then
        echo $TOPIC6
          until ./bin/kafka-topics.sh --create --replication-factor 1 --partitions 1 --zookeeper $ZOOKEEPER --topic $TOPIC6; do
            echo Retrying creation of topic $TOPIC6
            sleep 10
          done
    fi
    if [ -n "$TOPIC7" ]; then
        echo $TOPIC7
          until ./bin/kafka-topics.sh --create --replication-factor 1 --partitions 1 --zookeeper $ZOOKEEPER --topic $TOPIC7; do
            echo Retrying creation of topic $TOPIC7
            sleep 10
          done
    fi
    if [ -n "$TOPIC8" ]; then
        echo $TOPIC8
          until ./bin/kafka-topics.sh --create --replication-factor 1 --partitions 1 --zookeeper $ZOOKEEPER --topic $TOPIC8; do
            echo Retrying creation of topic $TOPIC8
            sleep 10
          done
    fi
    ./bin/kafka-topics.sh --zookeeper $ZOOKEEPER --list
fi
