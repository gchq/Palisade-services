package uk.gov.gchq.palisade.service.user.exception;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.hamcrest.MatcherAssert;
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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;

@RunWith(JUnit4.class)
public class NoSuchUserIdExceptionTest {

    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @Before
    public void setup() {
        logger = (Logger) LoggerFactory.getLogger(NoSuchUserIdException.class);
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
    public void NoSuchUserIdExceptionTest() {
        final NoSuchUserIdException noSuchUserIdException = new NoSuchUserIdException("NoSuchUser");
        assertThat(noSuchUserIdException.getMessage(), is(equalTo("NoSuchUser")));
        List<String> debugMessages = getMessages(event -> event.getLevel() == Level.INFO);
        assertNotEquals(0, debugMessages.size());
        MatcherAssert.assertThat(debugMessages, Matchers.hasItems(
                Matchers.containsString("NoSuchUserIdException thrown with")
        ));

    }
}
