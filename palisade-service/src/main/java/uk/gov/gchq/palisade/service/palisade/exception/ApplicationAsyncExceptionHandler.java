package uk.gov.gchq.palisade.service.palisade.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;

import java.lang.reflect.Method;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ApplicationAsyncExceptionHandler implements AsyncUncaughtExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationAsyncExceptionHandler.class);

    @Override
    public void handleUncaughtException(Throwable throwable, Method method, Object... objects) {
        String params = Stream.of(objects).map(Object::toString).collect(Collectors.joining(", ", "[", "]"));

        LOGGER.error("Uncaught Exception thrown by Async method [{}] : {} with Parameters: {}", method.getName(), throwable.getMessage(), params);
    }

}
