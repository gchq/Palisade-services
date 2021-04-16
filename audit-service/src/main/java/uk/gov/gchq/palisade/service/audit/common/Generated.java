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

package uk.gov.gchq.palisade.service.audit.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/*
 * Used by JaCoCo and SonarQube, any method annotated with an annotation with
 * a simple name of "Generated' is ignored from code coverage reports. For best
 * results, this should be integrated into one's IDE. In IntelliJ, this is done
 * through: Code ->
 *          Generate ->
 *          [method] ->
 *          ... -
 *          Prepend "@uk.gov.gchq.palisade.service.audit.common.Generated" to the velocity template.
 *
 * Alternatively, xml files representing these code generation templates can
 * be found under ~/.IntelliJIdea${year.version}/config/options:
 * - equalsHashCodeTemplates.xml
 * - getterTemplates.xml
 * - setterTemplates.xml
 * - toStringTemplates.xml
 *
 * It is recommended to include this in all code generation methods used, such
 * as: equals, hashCode, toString, getters, setters
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Generated {
}
