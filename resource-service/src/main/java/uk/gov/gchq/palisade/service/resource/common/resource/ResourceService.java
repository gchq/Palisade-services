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

package uk.gov.gchq.palisade.service.resource.common.resource;

import java.util.Iterator;

/**
 * The resource service is the Palisade component that determines what resources are available that meet a specific
 * (type of) request and how they should be accessed. This interface details several methods for obtaining a list of
 * resources, e.g. by type or by data format. The methods of this service all return {@link Iterator}s which link a valid
 * {@link LeafResource} with a {@link ConnectionDetail} object. The ${@link ConnectionDetail} objects contain
 * information on how to set up a connection to retrieve a particular resource. Implementations of this service do not
 * deal with the filtering or application of security policy to the resources. Therefore, a result returned from a
 * method call on this interface doesn't guarantee that the user will be allowed to access it by policy. Other
 * components of the Palisade system will enforce the necessary policy controls to prevent access to resources by users
 * without the necessary access rights.
 * Implementation note: None of the ${@code getResourcesByXXX} methods in this class will return in error if there
 * don't happen to be any resources that do not match a request, instead they will simply return empty ${@link Iterator}
 * instances.
 */
public interface ResourceService {

    /**
     * Get a list of resources based on a specific resource. This allows for the retrieval of the appropriate {@link
     * ConnectionDetail}s for a given resource. It may also be used to retrieve the details all the resources that are
     * notionally children of another resource. For example, in a standard hierarchical filing system the files in a
     * directory could be considered child resources and calling this method on the directory resource would fetch the
     * details on the contained files.
     *
     * @param resource the resource to request
     * @return an {@link Iterator} of resources, each with an appropriate {@link ConnectionDetail}
     */
    Iterator<LeafResource> getResourcesByResource(final Resource resource);

    /**
     * Retrieve resource and connection details by resource ID. The request object allows the client to specify the
     * resource ID and obtain the connection details once the returned future has completed.
     *
     * @param resourceId the ID to request
     * @return an {@link Iterator} of resources, each with an appropriate {@link ConnectionDetail}
     */
    Iterator<LeafResource> getResourcesById(final String resourceId);

    /**
     * Obtain a list of resources that match a specific resource type. This method allows a client to obtain potentially
     * large collections of resources by requesting all the resources of one particular type. For example, a client may
     * request all "employee contact card" records. Please note the warning in the class documentation above, that just
     * because a resource is available does not guarantee that the requesting client has the right to access it.
     *
     * @param type the type of resource to retrieve.
     * @return an {@link Iterator} of resources, each with an appropriate {@link ConnectionDetail}
     */
    Iterator<LeafResource> getResourcesByType(final String type);

    /**
     * Find all resources that match a particular data format. Resources of a particular data format may not share a
     * type, e.g. not all CSV format records will contain employee contact details. This method allows clients to
     * retrieve all the resources Palisade knows about that conform to one particular format. Note that this method can
     * potentially return large ${@code Map}s with many mappings.
     *
     * @param serialisedFormat the specific format for retrieval
     * @return an {@link Iterator} of resources, each with an appropriate {@link ConnectionDetail}
     */
    Iterator<LeafResource> getResourcesBySerialisedFormat(final String serialisedFormat);

    /**
     * Informs Palisade about a specific resource that it may return to users. This lets Palisade clients request access
     * to that resource and allows Palisade to provide policy controlled access to it via the other methods in this
     * interface.
     *
     * @param resource the resource that Palisade can manage access to
     * @return whether or not the addResource call completed successfully
     */
    Boolean addResource(final LeafResource resource);

}
