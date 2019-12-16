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

package uk.gov.gchq.palisade.service.policy.exception;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotEquals;

@RunWith(JUnit4.class)
public class NoPolicyExceptionTest {

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @Before
    public void setup() {
        logger = (Logger) LoggerFactory.getLogger(NoPolicyException.class);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @After
    public void tearDown() {
        logger.detachAppender(appender);
        appender.stop();
    }

    private List<String> getMessages(Predicate<ILoggingEvent> predicate) {
        return appender.list.stream()
                .filter(predicate)
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
    }

    @Test
    public void NoPolicyExceptionMessage() {
        final NoPolicyException noPolicyException = new NoPolicyException("noSuchPolicy");
        assertThat(noPolicyException.getMessage(), is(equalTo("noSuchPolicy")));
        List<String> debugMessages = getMessages(event -> event.getLevel() == Level.INFO);
        assertNotEquals(0, debugMessages.size());
        assertThat(debugMessages, Matchers.hasItems(
                Matchers.containsString("NoPolicyException thrown with message")
        ));
    }

    @Test
    public void NoPolicyExceptionThrowable() {
        final NoPolicyException noPolicyException = new NoPolicyException(new Throwable("noSuchThrowable"));
        List<String> debugMessages = getMessages(event -> event.getLevel() == Level.INFO);
        assertNotEquals(0, debugMessages.size());
        assertThat(debugMessages, Matchers.hasItems(
                Matchers.containsString("NoPolicyException thrown with throwable")
        ));
    }

    @Test
    public void NoPolicyExceptionMessageAndThrowable() {
        final NoPolicyException noPolicyException = new NoPolicyException("noSuchPolicy", new Throwable("noSuchThrowable"));
        assertThat(noPolicyException.getMessage(), is(equalTo("noSuchPolicy")));
        List<String> debugMessages = getMessages(event -> event.getLevel() == Level.INFO);
        assertNotEquals(0, debugMessages.size());
        assertThat(debugMessages, Matchers.hasItems(
                Matchers.containsString("NoPolicyException thrown with message noSuchPolicy, and throwable")
        ));
    }
}


