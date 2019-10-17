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

package uk.gov.gchq.palisade.service.resource.service;

import uk.gov.gchq.palisade.ToStringBuilder;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;

public class ResourceDetails {

    public static final Pattern FILENAME_PATTERN = Pattern.compile("(?<type>.+)_(?<name>.+)\\.(?<format>.+)");
    public static final String FORMAT = "TYPE_FILENAME.FORMAT";
    private String fileName, type, format;

    public ResourceDetails (final String fileName, final String type, final String format) {
        requireNonNull(fileName, "fileName");
        requireNonNull(type, "type");
        requireNonNull(format, "format");
        this.fileName = fileName;
        this.type = type;
        this.format = format;
    }

    public String getFileName() {
        return fileName;
    }

    public String getType() {
        return type;
    }

    public String getFormat() {
        return format;
    }

    protected static ResourceDetails getResourceDetailsFromFileName(final String fileName) {
        //get filename component
        final String[] split = fileName.split(Pattern.quote("/"));
        final String fileString = split[split.length - 1];
        //check match
        Matcher match = validateNameRegex(fileString);
        if (!match.matches()) {
            throw new IllegalArgumentException("Filename doesn't comply with " + FORMAT + ": " + fileName);
        }

        return new ResourceDetails(fileName, match.group("type"), match.group("format"));
    }

    public static boolean isValidResourceName(final String fileName) {
        requireNonNull(fileName);
        return validateNameRegex(fileName).matches();
    }

    private static Matcher validateNameRegex(final String fileName) {
        return FILENAME_PATTERN.matcher(fileName);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("type", type)
                .append("fileName", fileName)
                .append("format", format)
                .build();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof ResourceDetails)) return false;
        ResourceDetails that = (ResourceDetails) o;
        boolean fileName = getFileName().equals(that.getFileName());
        boolean type = getType().equals(that.getType());
        boolean format = getFormat().equals(that.getFormat());
        return fileName && type && format;
    }

    @Override
    public int hashCode() {
        return Objects.hash(getFileName(), getFormat(),  getType());
    }
}
