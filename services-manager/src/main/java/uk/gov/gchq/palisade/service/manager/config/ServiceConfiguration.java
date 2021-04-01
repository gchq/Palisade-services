/*
 * Copyright 2018-2021 Crown Copyright
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

import uk.gov.gchq.palisade.service.manager.common.Generated;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;

import static java.util.Objects.requireNonNull;

public class ServiceConfiguration {
    private static final String SPRING_LIST_SEP = ",";

    private String jar;
    private List<String> paths = Collections.emptyList();
    private List<String> profiles = Collections.singletonList("default");
    private Optional<String> log = Optional.empty();
    private Optional<String> err = Optional.empty();
    private Map<String, String> level = Collections.emptyMap();

    @Generated
    public String getJar() {
        return jar;
    }

    @Generated
    public void setJar(final String jar) {
        requireNonNull(jar);
        this.jar = jar;
    }

    @Generated
    public List<String> getPaths() {
        return paths;
    }

    @Generated
    public void setPaths(final List<String> paths) {
        requireNonNull(paths);
        this.paths = paths;
    }

    @Generated
    public List<String> getProfiles() {
        return profiles;
    }

    @Generated
    public void setProfiles(final List<String> profiles) {
        requireNonNull(profiles);
        this.profiles = profiles;
    }

    @Generated
    public Optional<String> getLog() {
        return log;
    }

    @Generated
    public void setLog(final Optional<String> log) {
        requireNonNull(log);
        this.log = log;
    }

    @Generated
    public Optional<String> getErr() {
        return err;
    }

    @Generated
    public void setErr(final Optional<String> err) {
        requireNonNull(err);
        this.err = err;
    }

    @Generated
    public Map<String, String> getLevel() {
        return level;
    }

    @Generated
    public void setLevel(final Map<String, String> level) {
        requireNonNull(level);
        this.level = level;
    }

    /**
     * Given a yaml configuration for a service, produce a ProcessBuilder for running this service
     *
     * @return a ProcessBuilder which may be .start()ed to spawn a new JVM
     */
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
        log.ifPresent(logFile -> pb.redirectOutput(new File(logFile)));
        err.ifPresent(errorFile -> pb.redirectError(new File(errorFile)));

        return pb;
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ServiceConfiguration)) {
            return false;
        }
        final ServiceConfiguration that = (ServiceConfiguration) o;
        return Objects.equals(jar, that.jar) &&
                Objects.equals(paths, that.paths) &&
                Objects.equals(profiles, that.profiles) &&
                Objects.equals(log, that.log) &&
                Objects.equals(err, that.err) &&
                Objects.equals(level, that.level);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(jar, paths, profiles, log, err, level);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", ServiceConfiguration.class.getSimpleName() + "[", "\n]")
                .add("\n\tjar='" + jar + "'")
                .add("\n\tpaths=" + paths)
                .add("\n\tprofiles=" + profiles)
                .add("\n\tlog=" + log)
                .add("\n\terr=" + err)
                .add("\n\tlevel=" + level)
                .toString();
    }
}
