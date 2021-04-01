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
package uk.gov.gchq.palisade.contract.policy.common;

import uk.gov.gchq.palisade.service.policy.common.resource.LeafResource;
import uk.gov.gchq.palisade.service.policy.common.resource.impl.DirectoryResource;
import uk.gov.gchq.palisade.service.policy.common.resource.impl.FileResource;
import uk.gov.gchq.palisade.service.policy.common.resource.impl.SystemResource;
import uk.gov.gchq.palisade.service.policy.common.rule.IsTextResourceRule;
import uk.gov.gchq.palisade.service.policy.common.rule.PassThroughRule;
import uk.gov.gchq.palisade.service.policy.common.rule.PredicateRule;
import uk.gov.gchq.palisade.service.policy.common.rule.Rules;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PolicyTestCommon {
    /**
     * Setup a collection of resources with policies like so:
     * /txt - only txt type files are viewable
     * ../txt/json - only json format files are viewable
     * ..../txt/json/json.txt - an accessible json txt file
     * ..../txt/json/json.avro - an inaccessible json avro file (breaks /txt rule)
     * ..../txt/json/pickled.txt - an inaccessible pickle txt file (breaks /txt/json rule)
     * ../txt/sensitive - only users with sensitive auth can view
     * ..../txt/sensitive/report.txt - an accessible (to sensitive auths) txt file
     * ..../txt/sensitive/salary.csv - an inaccessible csv file (breaks /txt rule)
     * ../txt/secret - only users with secret auth can view, a purpose of testing will redact all record-level info
     * ..../txt/secret/secrets.txt - an accessible (to secret auths) txt file
     * /new - a directory to be added with a pass-thru policy (do nothing)
     * ../new/file.exe - an accessible executable (not under /txt policy)
     **/

    // A system that only allows text files to be seen
    public static final SystemResource TXT_SYSTEM = new SystemResource().id("/txt");
    public static final Rules<LeafResource> TXT_POLICY = new Rules<LeafResource>()
            .addRule("Resource serialised format is txt", new IsTextResourceRule());

    // A directory that only allows JSON types
    public static final DirectoryResource JSON_DIRECTORY = new DirectoryResource().id("/txt/json").parent(TXT_SYSTEM);
    public static final Rules<LeafResource> JSON_POLICY = new Rules<LeafResource>()
            .addRule("Resource type is json", (PredicateRule<LeafResource>) (resource, user, context) -> resource.getType().equals("json"));

    // A text file containing json data - this should be accessible
    public static final FileResource ACCESSIBLE_JSON_TXT_FILE = new FileResource().id("/txt/json/json.txt").serialisedFormat("txt").type("json").parent(JSON_DIRECTORY);

    // A secret directory that allows only secret authorised users
    public static final DirectoryResource SECRET_DIRECTORY = new DirectoryResource().id("/txt/secret").parent(TXT_SYSTEM);
    public static final Rules<LeafResource> SECRET_POLICY = new Rules<LeafResource>()
            .addRule("Check user has 'Secret' auth", (PredicateRule<LeafResource>) (resource, user, context) -> user.getAuths().contains("Secret"))
            .addPredicateRule("Redact all with 'Testing' purpose", (record, user, context) -> !context.getPurpose().equals("Testing"));

    // A secret file - accessible only to the secret user
    public static final FileResource SECRET_TXT_FILE = new FileResource().id("/txt/secret/secrets.txt").serialisedFormat("txt").type("txt").parent(SECRET_DIRECTORY);

    public static final FileResource NEW_FILE = new FileResource().id("/new/file.exe").serialisedFormat("exe").type("elf").parent(new SystemResource().id("/new"));

    // A do-nothing policy to apply to leaf resources
    public static final Rules<LeafResource> PASS_THROUGH_POLICY = new Rules<LeafResource>()
            .addRule("Does nothing", new PassThroughRule<>());

    public static final Set<FileResource> FILE_RESOURCES = new HashSet<>(Arrays.asList(ACCESSIBLE_JSON_TXT_FILE, SECRET_TXT_FILE));
}

