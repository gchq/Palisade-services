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
FROM openjdk:11@sha256:736dd07fc5cb53060a49d38c86b9e673974510c5a18afc40b1c936475f59aee7

ARG NETCAT_VERSION=1.10-41
RUN apt-get update \
   && apt-get install -y netcat=${NETCAT_VERSION} --no-install-recommends \
   && apt-get clean \
   && rm -rf /var/lib/apt/lists/*

ARG KAFKA_VERSION=2.2.1
ARG SCALA_VERSION=2.12
RUN curl -LO  https://archive.apache.org/dist/kafka/${KAFKA_VERSION}/kafka_${SCALA_VERSION}-${KAFKA_VERSION}.tgz \
   && tar --strip-components=1 -xvzf kafka_${SCALA_VERSION}-${KAFKA_VERSION}.tgz \
   && rm kafka_${SCALA_VERSION}-${KAFKA_VERSION}.tgz

COPY wait-for-services.sh create-topics.sh /bin/
RUN sed -i 's/\r$//' /bin/wait-for-services.sh /bin/create-topics.sh
