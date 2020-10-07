package uk.gov.gchq.palisade.service.topicoffset.service;

import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.header.internals.RecordHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import uk.gov.gchq.palisade.service.topicoffset.model.StreamMarker;
import uk.gov.gchq.palisade.service.topicoffset.model.Token;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Set of tests that cover three possible uses of the SimpleTopicOffsetService method createTopicOffsetResponse.
 * Consists of:
 * 1) Indicator which is for the start of a set of messages for a specific request
 * 2) Indicator which is for the end of a set of messages for a specific request
 * 3) No Indicator which is for the messages with data
 */
class SimpleTopicOffsetServiceTest {

    private final SimpleTopicOffsetService simpleTopicOffsetService = new SimpleTopicOffsetService();

    public static final String REQUEST_TOKEN = "test-request-token";
    private Headers headers;

    @BeforeEach
    void startUP() {
        headers = new RecordHeaders();
        headers.add(Token.HEADER, REQUEST_TOKEN.getBytes());
    }

    /**
     * Tests the simpleTopicOffsetService method isOffsetForTopic will return true when the {@link StreamMarker} is
     * set to the start value
     */
    @Test
    void testTopicOffsetServiceWithAStart() {
        headers.add(StreamMarker.HEADER, StreamMarker.START.toString().getBytes());
        assertThat(simpleTopicOffsetService.isOffsetForTopic(headers)).isTrue();
    }

    /**
     * Tests the simpleTopicOffsetService method isOffsetForTopic will return false when the {@link StreamMarker} is
     * set to the end value
     */
    @Test
    void testTopicOffsetServiceWithAnEnd() {
        headers.add(StreamMarker.HEADER, StreamMarker.END.toString().getBytes());
        assertThat(simpleTopicOffsetService.isOffsetForTopic(headers)).isFalse();
    }

    /**
     * Tests the simpleTopicOffsetService method isOffsetForTopic will return false when there is no stream marker
     */
    @Test
    void testTopicOffsetServiceWithoutAnyStreamMarker() {
        assertThat(simpleTopicOffsetService.isOffsetForTopic(headers)).isFalse();
    }
}