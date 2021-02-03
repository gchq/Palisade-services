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

VOLUME /tmp

ARG JAR_FILE
COPY $JAR_FILE /usr/share/filtered-resource-service/filtered-resource-service.jar

LABEL service="filtered-resource" project="palisade"

ENTRYPOINT ["java", "-jar", "/usr/share/filtered-resource-service/filtered-resource-service.jar"]
