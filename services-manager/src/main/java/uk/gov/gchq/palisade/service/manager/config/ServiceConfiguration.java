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

    private List<String> classpath = Collections.emptyList();
    private Optional<String> config = Optional.empty();
    private Optional<String> launcher = Optional.empty();
    private Optional<String> main = Optional.empty();
    private List<String> profiles = Collections.singletonList("default");
    private Optional<String> log = Optional.empty();
    private Optional<String> err = Optional.empty();
    private Map<String, String> level = Collections.emptyMap();

    public ProcessBuilder getProcessBuilder() {
        ArrayList<String> command = new ArrayList<>();
        command.add("java");
        if (!classpath.isEmpty()) {
            command.add("-cp");
            command.add(String.join(File.pathSeparator, classpath));
        }
        config.ifPresent(config -> command.add(String.format("-Dspring.location=%s", config)));
        command.add(String.format("-Dspring.profiles.active=%s", String.join(SPRING_LIST_SEP, profiles)));
        level.forEach((clazz, level) -> command.add(String.format("-Dlogging.level.%s=%s", clazz, level)));
        main.ifPresent(main -> command.add(String.format("-Dloader.main=%s", main)));
        launcher.ifPresent(command::add);

        ProcessBuilder pb = new ProcessBuilder().command(command);
        log.ifPresent(log -> pb.redirectOutput(new File(log)));
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

    public void setClasspath(final List<String> classpath) {
        this.classpath = classpath;
    }

    public void setConfig(final String config) {
        this.config = Optional.ofNullable(config);
    }

    public void setLauncher(final String launcher) {
        this.launcher = Optional.ofNullable(launcher);
    }

    public void setMain(final String main) {
        this.main = Optional.ofNullable(main);
    }

    public void setProfiles(final List<String> profiles) {
        this.profiles = profiles;
    }

    public void setLog(final String log) {
        this.log = Optional.ofNullable(log);
    }

    public void setErr(final String err) {
        this.err = Optional.ofNullable(err);
    }

    public void setLevel(final Map<String, String> level) {
        this.level = level;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RunnerConfiguration{\n");
        sb.append("\tclasspath=").append(classpath).append('\n');
        sb.append("\tconfig=").append(config).append('\n');
        sb.append("\tlauncher=").append(launcher).append('\n');
        sb.append("\tmain=").append(main).append('\n');
        sb.append("\tprofiles=").append(profiles).append('\n');
        sb.append("\tlog=").append(log).append('\n');
        sb.append("\terr=").append(err).append('\n');
        sb.append("\tlevel=").append(level).append('\n');
        sb.append('}');
        return sb.toString();
    }
}
