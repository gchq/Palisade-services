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

package uk.gov.gchq.palisade.contract.data.config.model;


import uk.gov.gchq.palisade.service.data.common.Generated;

import java.io.Serializable;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * A simple 'Employee' type to read from a file, with some 'interesting' default values
 */
public class Employee implements Serializable {
    private static final long serialVersionUID = 1;

    private String name;
    private Integer number;

    public Employee() {
        this.name = "the only employee";
        this.number = 1;
    }

    public Employee(final String name, final Integer number) {
        this.name = name;
        this.number = number;
    }

    @Generated
    public String getName() {
        return name;
    }

    @Generated
    public void setName(final String name) {
        this.name = name;
    }

    @Generated
    public Integer getNumber() {
        return number;
    }

    @Generated
    public void setNumber(final Integer number) {
        this.number = number;
    }

    @Override
    @Generated
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Employee)) {
            return false;
        }
        final Employee employee = (Employee) o;
        return Objects.equals(name, employee.name) &&
                Objects.equals(number, employee.number);
    }

    @Override
    @Generated
    public int hashCode() {
        return Objects.hash(name, number);
    }

    @Override
    @Generated
    public String toString() {
        return new StringJoiner(", ", Employee.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("number=" + number)
                .toString();
    }
}
