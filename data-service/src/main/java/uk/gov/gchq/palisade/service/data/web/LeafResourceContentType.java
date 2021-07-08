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

package uk.gov.gchq.palisade.client.s3.web;

import akka.http.javadsl.model.ContentType;
import akka.http.javadsl.model.ContentTypes;
import akka.http.javadsl.model.MediaType;
import akka.http.javadsl.model.MediaTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.gov.gchq.palisade.resource.LeafResource;

import java.util.Arrays;

final class LeafResourceContentType {
    private static final Logger LOGGER = LoggerFactory.getLogger(LeafResourceContentType.class);

    private LeafResourceContentType() {
        // Hide public constructor for utility class
    }

    /**
     * Get a mediaType from a string, which expects the format "mainType/subType"
     *
     * @param mediaTypeStr the mediaType string
     * @return the same mediaType, but as a rich object
     */
    public static MediaType.Binary mediaType(final String mediaTypeStr) {
        String[] parts = mediaTypeStr.split("/");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Parts for format '" + mediaTypeStr + "' was now two: " + Arrays.toString(parts));
        }

        String mainType = parts[0];
        String subType = parts[1];

        LOGGER.trace("Decoded media-type string '{}' into main-type '{}' and sub-type '{}'", mediaTypeStr, mainType, subType);
        return MediaTypes.customBinary(mainType, subType, false);
    }

    /**
     * Get a contentType from a string, which expects the format "mediaType; charset" or "mediaType"
     *
     * @param contentTypeStr the contentType string
     * @return the same contentType, but as a rich object
     */
    public static ContentType contentType(final String contentTypeStr) {
        String[] parts = contentTypeStr.split(";");
        if (parts.length != 1 && parts.length != 2) {
            throw new IllegalArgumentException("Parts for format '" + contentTypeStr + "' was not one or two: " + Arrays.toString(parts));
        }

        String mediaTypeStr = parts[0];
        String charset = null;
        if (parts.length == 2) {
            charset = parts[1];
        }

        LOGGER.trace("Decoded content-type string '{}' into media-type string '{}' and charset '{}'", contentTypeStr, mediaTypeStr, charset);
        return ContentTypes.create(mediaType(mediaTypeStr));
    }

    /**
     * Create a {@link ContentType} from a {@link LeafResource} using the serialised format (which should already be a valid contentType string)
     *
     * @param leafResource the leafResource to get a contentType for
     * @return the appropriate contentType for the serialisedFormat string
     */
    public static ContentType create(final LeafResource leafResource) {
        return contentType(leafResource.getSerialisedFormat());
    }
}
