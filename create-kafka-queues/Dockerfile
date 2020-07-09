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
FROM openjdk:8-alpine

#RUN apk update
#Search for a particular version of curl
#RUN apk search -v 'curl*'
RUN apk --no-cache add vim=8.1.1365-r0
RUN apk --no-cache add curl=7.64.0-r3
RUN apk --no-cache add bash=4.4.19-r1

RUN curl -LO  http://archive.apache.org/dist/kafka/2.2.1/kafka_2.12-2.2.1.tgz \
   && tar --strip-components=1 -xvzf kafka_2.12-2.2.1.tgz  && rm kafka_2.12-2.2.1.tgz
COPY create-topics.sh /bin/create-topics.sh
