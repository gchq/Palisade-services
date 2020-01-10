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

import org.apache.tools.ant.util.JavaEnvUtils;

import java.io.File;

public class RunnerConfiguration {

    private String classpath;
    private String config;
    private String launcher;
    private String main;
    private String profiles;
    private String log;

    ProcessBuilder getProcessBuilder() {
        String[] runnerCommand = new String[]{
                JavaEnvUtils.getJreExecutable("java"),
                "-cp", getClasspath(),
                String.format("-Dspring.location=%s", getConfig()),
                String.format("-Dspring.profiles.active=%s", getProfiles()),
                String.format("-Dloader.main=%s", getMain()),
                getLauncher()
        };
        ProcessBuilder builder = new ProcessBuilder()
                .command(runnerCommand)
                .redirectOutput(new File(getLog()))
                .redirectError(new File(getLog()));
        return builder;
    }

    public String getClasspath() {
        return classpath;
    }

    public void setClasspath(final String classpath) {
        this.classpath = classpath;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(final String config) {
        this.config = config;
    }

    public String getLauncher() {
        return launcher;
    }

    public void setLauncher(final String launcher) {
        this.launcher = launcher;
    }

    public String getMain() {
        return main;
    }

    public void setMain(final String main) {
        this.main = main;
    }

    public String getProfiles() {
        return profiles;
    }

    public void setProfiles(final String profiles) {
        this.profiles = profiles;
    }

    public String getLog() {
        return log;
    }

    public void setLog(final String log) {
        this.log = log;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("RunnerConfiguration{");
        sb.append("classpath='").append(classpath).append('\'');
        sb.append(", config='").append(config).append('\'');
        sb.append(", launcher='").append(launcher).append('\'');
        sb.append(", main='").append(main).append('\'');
        sb.append(", profiles='").append(profiles).append('\'');
        sb.append(", log='").append(log).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
