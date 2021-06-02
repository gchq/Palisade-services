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

ARG BASE_IMG
FROM $BASE_IMG

ARG NETCAT_VERSION=1.10-41.1
ARG CURL_VERSION=~7.64.0-4+deb10u2
RUN apt-get update \
   && apt-get install -y netcat=${NETCAT_VERSION} curl=${CURL_VERSION} --no-install-recommends \
   && apt-get clean \
   && rm -rf /var/lib/apt/lists/*

ARG KAFKA_TGZ_URL
RUN echo ${KAFKA_TGZ_URL}
RUN curl --location ${KAFKA_TGZ_URL} --output kafka.tgz && \
    tar --strip-components=1 -xvzf kafka.tgz && \
    rm kafka.tgz

COPY wait-for-services.sh create-topics.sh /bin/
RUN sed -i 's/\r$//' /bin/wait-for-services.sh /bin/create-topics.sh
