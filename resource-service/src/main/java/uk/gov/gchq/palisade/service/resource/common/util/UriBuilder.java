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

package uk.gov.gchq.palisade.service.resource.common.util;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

/**
 * Convenience wrapper around the {@link URI} constructors.
 * Allows constructing new URIs from an immutable baseUri, with appropriate optionals for each part.
 */
public class UriBuilder {

    private Optional<URI> baseUri = Optional.empty();

    /**
     * Taking a baseUri, creates a properly formatted URI
     *
     * @param baseUri an unformatted uri
     * @return a properly formatted uri
     */
    public static IScheme create(final URI baseUri) {
        UriBuilder builder = new UriBuilder();
        builder.baseUri = Optional.of(baseUri);
        return builder.build();
    }

    /**
     * The starter method for the Builder class.
     *
     * @return the composed immutable object
     */
    public static IScheme create() {
        return new UriBuilder().build();
    }

    /**
     * Starter method for the Builder class.  This method is called to start the process of creating the
     * URI class.
     *
     * @return interface {@link IScheme} for the next step in the build.
     */
    private IScheme build() {
        return scheme -> authority -> path -> query -> (String fragment) -> {
            String thisScheme = Optional.ofNullable(scheme).or(() -> baseUri.map(URI::getScheme)).orElseThrow();
            String thisAuth = Optional.ofNullable(authority).or(() -> baseUri.map(URI::getAuthority)).orElse(null);
            String thisPath = Optional.ofNullable(path).or(() -> baseUri.map(URI::getPath)).orElseThrow();
            String thisQuery = Optional.ofNullable(query).or(() -> baseUri.map(URI::getQuery)).orElse(null);
            String thisFrag = Optional.ofNullable(fragment).or(() -> baseUri.map(URI::getFragment)).orElse(null);
            try {
                return new URI(
                        thisScheme,
                        thisAuth,
                        thisPath,
                        thisQuery,
                        thisFrag
                );
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException("Invalid URI: " + thisScheme + ":" + thisAuth + "" + thisPath + "?" + thisQuery + "#" + thisFrag, e);
            }
        };
    }

    /**
     * Adds the scheme to the URI
     */
    public interface IScheme {
        /**
         * Adds the {@link URI#getScheme()} to the URI
         *
         * @param scheme the string scheme for the uri
         * @return interface {@link IAuthority} for the next step in the build.
         */
        IAuthority withScheme(String scheme);

        /**
         * By default, create the uri without a scheme
         *
         * @return interface {@link IAuthority} for the next step in the build.
         */
        default IAuthority withoutScheme() {
            return withScheme(null);
        }
    }

    /**
     * Adds the authority to the URI
     */
    public interface IAuthority {
        /**
         * Adds the {@link URI#getAuthority()} to the URI
         *
         * @param authority the string authority for the uri
         * @return interface {@link IPath} for the next step in the build.
         */
        IPath withAuthority(String authority);

        /**
         * By default, create a uri without the authority
         *
         * @return interface {@link IPath} for the next step in the build
         */
        default IPath withoutAuthority() {
            return withAuthority(null);
        }
    }

    /**
     * Adds the path to the URI
     */
    public interface IPath {
        /**
         * Adds the {@link URI#getPath()} to the URI
         *
         * @param path the string path for the uri
         * @return interface {@link IQuery} for the next step in the build.
         */
        IQuery withPath(String path);

        /**
         * By default, create a uri without the path
         *
         * @return interface {@link IQuery} for the next step in the build
         */
        default IQuery withoutPath() {
            return withPath(null);
        }
    }

    /**
     * Adds the query to the URI
     */
    public interface IQuery {
        /**
         * Adds the {@link URI#getQuery()} to the URI
         *
         * @param query the string query for the uri
         * @return interface {@link IFragment} for the next step in the build.
         */
        IFragment withQuery(String query);

        /**
         * By default, create a uri without the query
         *
         * @return interface {@link IFragment} for the next step in the build
         */
        default IFragment withoutQuery() {
            return withQuery(null);
        }
    }

    /**
     * Adds the fragment to the URI
     */
    public interface IFragment {
        /**
         * Adds the {@link URI#getFragment()} to the URI
         *
         * @param fragment the string fragment for the uri
         * @return a now completed URI object
         */
        URI withFragment(String fragment);

        /**
         * By default, create a uri without a fragment
         *
         * @return a now completed URI object
         */
        default URI withoutFragment() {
            return withFragment(null);
        }
    }

}
