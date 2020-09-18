package uk.gov.gchq.palisade.service.topicoffset.service;

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

    private SimpleTopicOffsetService simpleTopicOffsetService = new SimpleTopicOffsetService();

    public static final String REQUEST_TOKEN = "test-request-token";

    private Map<String, String> headers;

    @BeforeEach
    void startUP() {
        headers = new HashMap<>();
        headers.put(Token.HEADER, REQUEST_TOKEN);
    }

    /**
     * Tests the simpleTopicOffsetService method isOffsetForTopic will return true when the {@link StreamMarker} is
     * set to the start value
     *
     * @throws Exception fails to produce the expected results.
     */
    @Test
    void testTopicOffsetServiceWithAStart() throws Exception {

        headers.put(StreamMarker.HEADER, StreamMarker.START.toString());
        assertThat(simpleTopicOffsetService.isOffsetForTopic(headers)).isTrue();

    }

    /**
     * Tests the simpleTopicOffsetService method isOffsetForTopic will return false when the {@link StreamMarker} is
     * set to the end value
     * @throws Exception fails to produce the expected results.
     */
    @Test
    void testTopicOffsetServiceWithAnEnd() throws Exception {

        headers.put(StreamMarker.HEADER, StreamMarker.END.toString());
        assertThat(simpleTopicOffsetService.isOffsetForTopic(headers)).isFalse();
    }

    /**
     * Tests the simpleTopicOffsetService method isOffsetForTopic will return false when there is no stream marker
     * @throws Exception fails to produce the expected results.
     */
    @Test
    void testTopicOffsetServiceWithoutAnyStreamMarker() throws Exception {

        assertThat(simpleTopicOffsetService.isOffsetForTopic(headers)).isFalse();
    }
}