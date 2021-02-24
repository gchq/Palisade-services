#! /bin/bash
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

wait_for_service() {
  name=$(echo $1 | cut -f 1 -d :)
  dest=$(echo $1 | cut -f 2 -d :)
  port=$(echo $1 | cut -f 3 -d :)
  until nc -vz $dest $port > /dev/null 2>&1; do
    >&2 echo "$(date) :: $name at $dest:$port is unavailable - sleeping"
    sleep 2
  done
  >&2 echo "$(date) :: $name at $dest:$port is up"
}

for service in "$@"
do
  wait_for_service $service
done
