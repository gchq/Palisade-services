/*
 * Copyright 2021 Crown Copyright
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

CREATE TABLE IF NOT EXISTS completeness
(
    `id`          INTEGER PRIMARY KEY,
    `entity_type` INTEGER,
    `entity_id`   VARCHAR(255)
);
CREATE TABLE IF NOT EXISTS resources
(
    `id`          INTEGER PRIMARY KEY AUTO_INCREMENT,
    `resource_id` VARCHAR(255) UNIQUE NOT NULL,
    `parent_id`   VARCHAR(255),
    `resource`    VARCHAR
);
CREATE TABLE IF NOT EXISTS types
(
    `id`          INTEGER PRIMARY KEY AUTO_INCREMENT,
    `resource_id` VARCHAR(255) UNIQUE NOT NULL,
    `type`        VARCHAR(255)
);
CREATE TABLE IF NOT EXISTS serialised_formats
(
    `id`                INTEGER PRIMARY KEY AUTO_INCREMENT,
    `resource_id`       VARCHAR(255) UNIQUE NOT NULL,
    `serialised_format` VARCHAR(255)
);
