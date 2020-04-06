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
#FROM openjdk:11@sha256:736dd07fc5cb53060a49d38c86b9e673974510c5a18afc40b1c936475f59aee7

FROM openjdk:8-alpine

RUN apk update
RUN apk add vim
RUN apk add curl

RUN curl -LO  https://archive.apache.org/dist/kafka/2.2.1/kafka_2.12-2.2.1.tgz \
   && tar -zxvf kafka_2.12-2.2.1.tgz


#VOLUME /tmp

#ARG JAR_FILE

#COPY $JAR_FILE /usr/share/palisade-service/palisade-service.jar

#COPY src/main/resources/log_level.sh /usr/share/palisade-service/log-level

#LABEL service="kafka-queues" project="kafka-queues"

#ENTRYPOINT ["java", "-classpath", "/usr/share/classpath", "-jar", "/usr/share/palisade-service/palisade-service.jar"]

