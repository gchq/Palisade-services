/*
 * Copyright 2019 Crown Copyright
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

package uk.gov.gchq.palisade.service.manager.config;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.File;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class ServiceConfiguration {
    private static final String SPRING_LIST_SEP = ",";

    private String jar;
    private List<String> paths = Collections.emptyList();
    private List<String> profiles = Collections.singletonList("default");
    private Optional<String> log = Optional.empty();
    private Optional<String> err = Optional.empty();
    private Map<String, String> level = Collections.emptyMap();

    public String getJar() {
        return jar;
    }

    public void setJar(final String jar) {
        this.jar = jar;
    }

    public List<String> getPaths() {
        return paths;
    }

    public void setPaths(final List<String> paths) {
        this.paths = paths;
    }

    public List<String> getProfiles() {
        return profiles;
    }

    public void setProfiles(final List<String> profiles) {
        this.profiles = profiles;
    }

    public Optional<String> getLog() {
        return log;
    }

    public void setLog(final Optional<String> log) {
        this.log = log;
    }

    public Optional<String> getErr() {
        return err;
    }

    public void setErr(final Optional<String> err) {
        this.err = err;
    }

    public Map<String, String> getLevel() {
        return level;
    }

    public void setLevel(final Map<String, String> level) {
        this.level = level;
    }

    public ProcessBuilder getProcessBuilder() {
        ArrayList<String> command = new ArrayList<>();
        // java (JVM)
        command.add("java");
        // -Dloader.path (include extra jars)
        if (!paths.isEmpty()) {
            command.add(String.format("-Dloader.path=%s", String.join(SPRING_LIST_SEP, paths)));
        }
        // -Dspring.profiles.active (spring profiles)
        if (!profiles.isEmpty()) {
            command.add(String.format("-Dspring.profiles.active=%s", String.join(SPRING_LIST_SEP, profiles)));
        }
        // -Dlogging.level (logging level)
        level.forEach((classpath, logLevel) ->
                command.add(String.format("-Dlogging.level.%s=%s", classpath, logLevel)));
        // -jar (jar to run)
        command.add("-jar");
        command.add(jar);

        ProcessBuilder pb = new ProcessBuilder().command(command);
        log.ifPresent(logged -> pb.redirectOutput(new File(logged)));
        err.ifPresent(err -> pb.redirectError(new File(err)));

        return pb;
    }

    public Map<String, HttpEntity<String>> getLoggingChangeEntities() {
        return level.entrySet().stream()
                .map(entry -> {
                    // Create a JSON object
                    HttpHeaders header = new HttpHeaders();
                    header.setContentType(MediaType.APPLICATION_JSON);
                    // Configure the endpoint to log to the new level
                    String body = String.format("{\"configuredLevel\":\"%s\"}", entry.getValue());
                    // POST the entity to the actuator, expect a response of OK
                    return new SimpleEntry<>(entry.getKey(), new HttpEntity<>(body, header));
                }).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RunnerConfiguration{\n");
        sb.append("\tjar=").append(jar).append('\n');
        sb.append("\tpaths=").append(paths).append('\n');
        sb.append("\tprofiles=").append(profiles).append('\n');
        sb.append("\tlog=").append(log).append('\n');
        sb.append("\terr=").append(err).append('\n');
        sb.append("\tlevel=").append(level).append('\n');
        sb.append('}');
        return sb.toString();
    }
}
