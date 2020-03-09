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
package uk.gov.gchq.palisade.service.palisade.domain;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import uk.gov.gchq.palisade.Context;
import uk.gov.gchq.palisade.RequestId;
import uk.gov.gchq.palisade.User;
import uk.gov.gchq.palisade.resource.LeafResource;
import uk.gov.gchq.palisade.resource.impl.DirectoryResource;
import uk.gov.gchq.palisade.resource.impl.FileResource;
import uk.gov.gchq.palisade.resource.impl.SystemResource;
import uk.gov.gchq.palisade.rule.Rule;
import uk.gov.gchq.palisade.rule.Rules;
import uk.gov.gchq.palisade.service.palisade.repository.LeafResourceRulesRepository;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD) //reset db after each test
@ActiveProfiles("dbtest")
public class LeafResourceRulesTest {

    private static class Employee {
        public String name;
        public String telephone;
        public String postcode;
    }

    private static class PhoneRule implements Rule<Employee> {
        @Override
        public Employee apply(Employee employee, User user, Context context) {
            employee.telephone = null;
            return employee;
        }
    }

    @Autowired
    private LeafResourceRulesRepository leafResourceRulesRepository;

    @Test
    public void storeAndRetrieveTest() {
        final FileResource fileResource = createFileResource("file:///organisation/dept/team/employee/john");
        final Rule<Employee> rule = new PhoneRule();
        Rules<Employee> resourceRules = new Rules<>();
        resourceRules.rule("phone-rule", rule);
        final LeafResourceRulesEntity entity = new LeafResourceRulesEntity(new RequestId().id("xxxx"), fileResource, resourceRules);

        this.leafResourceRulesRepository.save(entity);
        final SimpleImmutableEntry<LeafResource, Rules<?>> subject = this.leafResourceRulesRepository.getByRequestId("xxxx").stream().findFirst().orElse(new LeafResourceRulesEntity()).leafResourceRules();

        assertThat("The file id is not preserved through persistence", subject.getKey().getId(), is(equalTo(fileResource.getId())));
        assertThat("The rule id is not preserved through persistence", subject.getValue().getRules().keySet().stream().findFirst().orElse(""), is(equalTo("phone-rule")));
        assertThat("The rule type is not preserved through persistence", subject.getValue().getRules().values().stream().findFirst().filter(r -> r instanceof PhoneRule).isPresent(), is(true));
    }

    private FileResource createFileResource(final String id) {
        String path = id.substring(0, id.lastIndexOf("/") + 1);
        FileResource file = new FileResource().id(id).serialisedFormat("avro").type("employee");
        file.setParent(createParentResource(path));

        return file;
    }

    private DirectoryResource createParentResource(final String path) {
        String str = path;
        List<DirectoryResource> resourceList = new ArrayList<>();
        List<String> pathList = new ArrayList<>();

        do {
            pathList.add(str);
            str = str.substring(0, str.lastIndexOf("/"));
        } while (!str.endsWith("//"));

        for (String s : pathList) {
            DirectoryResource parentResource = addParentResource(s);
            if (!resourceList.isEmpty()) {
                resourceList.get(resourceList.size() - 1).setParent(parentResource);
            }
            resourceList.add(parentResource);
        }
        resourceList.get(resourceList.size() - 1).setParent(createSystemResource(str));

        return resourceList.get(0);
    }

    private DirectoryResource addParentResource(final String path) {
        return new DirectoryResource().id(path);
    }

    private SystemResource createSystemResource(final String path) {
        return new SystemResource().id(path);
    }

}
