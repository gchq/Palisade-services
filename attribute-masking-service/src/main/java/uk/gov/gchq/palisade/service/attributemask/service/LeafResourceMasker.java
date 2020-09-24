/*
 * Copyright 2020 Crown Copyright
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

package uk.gov.gchq.palisade.service.attributemask.service;

import uk.gov.gchq.palisade.resource.LeafResource;

import java.util.function.UnaryOperator;

/**
 * Mask the leafResource, removing any sensitive information - this may later include
 * applying a separate set of attributeRules, distinct from resourceRules and recordRules.
 * For now, it simply extends a  UnaryOperator interface
 */
public interface LeafResourceMasker extends UnaryOperator<LeafResource> {
}
