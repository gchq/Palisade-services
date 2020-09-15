package uk.gov.gchq.palisade.service.topicoffset.service;

import org.junit.jupiter.api.Test;
import uk.gov.gchq.palisade.service.topicoffset.request.StreamMarker;
import uk.gov.gchq.palisade.service.topicoffset.request.TopicOffsetResponse;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Set of tests that cover three possible uses of the SimpleTopicOffsetService method createTopicOffsetResponse.
 * Consists of:
 * 1) Indicator which is for the start of a set of messages for a specific request.
 * 2) Indicator which is for the end of a set of messages for a specific request
 * 3) No Indicator which is for the messages in-between (and have the data)
 */
class SimpleTopicOffsetServiceTest {

    /**
     * Tests the createTopicOffsetResponse method with the start message.  Only one that should produce a
     * TopicOffsetResponse as a response.
     * @throws Exception fails to produce the expected results.
     */
    @Test
    void testTopicOffsetServiceWithAStart() throws Exception {

        SimpleTopicOffsetService simpleTopicOffsetService = new SimpleTopicOffsetService();
        Optional<TopicOffsetResponse> topicOffsetOptional = simpleTopicOffsetService.createTopicOffsetResponse(StreamMarker.START);
        TopicOffsetResponse topicOffsetResponse =topicOffsetOptional.orElseThrow(() -> new RuntimeException("Should have produced a TopicOffsetResponse"));

        assertThat(topicOffsetResponse.getCommitOffset()).isNotZero();

    }

    /**
     * Tests the createTopicOffsetResponse method with the end message.  Should not produce a TopicOffsetResponse.
     * @throws Exception fails to produce the expected results.
     */
    @Test
    void testTopicOffsetServiceWithAnEnd() throws Exception {

        SimpleTopicOffsetService simpleTopicOffsetService = new SimpleTopicOffsetService();
        Optional<TopicOffsetResponse> topicOffsetOptional = simpleTopicOffsetService.createTopicOffsetResponse(StreamMarker.END);
        assertThat(topicOffsetOptional.isEmpty());

    }

    /**
     * Tests the createTopicOffsetResponse method without any kind of marker.  Should not produce a TopicOffsetResponse.
     * @throws Exception fails to produce the expected results.
     */
    @Test
    void testTopicOffsetServiceWithoutAnyStreamMarker() throws Exception {

        SimpleTopicOffsetService simpleTopicOffsetService = new SimpleTopicOffsetService();
        Optional<TopicOffsetResponse> topicOffsetOptional = simpleTopicOffsetService.createTopicOffsetResponse(null);
        assertThat(topicOffsetOptional.isEmpty());

    }
}