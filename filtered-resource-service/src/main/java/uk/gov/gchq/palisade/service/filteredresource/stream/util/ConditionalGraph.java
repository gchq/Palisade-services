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

package uk.gov.gchq.palisade.service.filteredresource.stream.util;

import akka.NotUsed;
import akka.japi.function.Function;
import akka.stream.FlowShape;
import akka.stream.Graph;
import akka.stream.UniformFanInShape;
import akka.stream.UniformFanOutShape;
import akka.stream.javadsl.Flow;
import akka.stream.javadsl.GraphDSL;
import akka.stream.javadsl.GraphDSL.Builder;
import akka.stream.javadsl.Merge;
import akka.stream.javadsl.Partition;

import java.util.Map;

/**
 * Class ConditionalGraph used in the {@link uk.gov.gchq.palisade.service.filteredresource.stream.config.AkkaRunnableGraph}
 */
public final class ConditionalGraph {

    private ConditionalGraph() {
        // No constructor for static helper class
    }

    /**
     * Helper function to selectively apply any number of {@link Flow}s conditionally by selecting using a map key function.
     * This is a wrapper around Akka's {@link Partition}, {@link Merge} and {@link GraphDSL} types.
     * For example, given a WebsocketMessage with a MessageType, perform a different function per type:
     * <pre>
     *   {@code
     *     Map<MessageType, Flow<~>> typeMap = Map.of(
     *         MessageType.ACK, (Flow<~>) service.onACK(),
     *         MessageType.SUB, (Flow<~>) service.onSUB()
     *     );
     *     Flow<WebsocketMessage, WebsocketMessage, NotUsed> flow = Flow.create()
     *             .via(ConditionalGraph.map(x -> x.getType().ordinal(), typeMap))
     *   }
     * </pre>
     * This allows executing different {@link Flow}s conditionally (eg. by client {@link uk.gov.gchq.palisade.service.filteredresource.model.MessageType}).
     *
     * @param keyFunc An injective function uniquely mapping each element of the flow to the keyspace.
     *                If the keyFunc is not surjective, then any elements with keys not mapped to a flow are dropped.
     * @param flowMap A map of keys to flows, such that keyFunc(elem) = x means x gets sent to flowMap[x]
     * @param <T>     The type of the flow's elements
     * @return a Flow from T to T applying conditionally each of the provided flows
     */
    public static <T> Graph<FlowShape<T, T>, NotUsed> map(final Function<T, Integer> keyFunc, final Map<Integer, Flow<T, T, NotUsed>> flowMap) {
        return Flow.fromGraph(GraphDSL.create(
                (Builder<NotUsed> builder) -> {
                    // Create strict ordering of keys - maps each key to each partition::out and merge::in port

                    // Create a fan-out and fan-in for each key-value in the map
                    UniformFanOutShape<T, T> partition = builder.add(Partition.create(flowMap.size(), keyFunc));
                    UniformFanInShape<T, T> merge = builder.add(Merge.create(flowMap.size()));

                    // Join up each partition port to its flow (from the map) and a merge port
                    flowMap.forEach((Integer key, Flow<T, T, NotUsed> flow) -> builder
                            .from(partition.out(key))
                            .via(builder.add(flow))
                            .toInlet(merge.in(key)));

                    // Return the flow
                    return FlowShape.of(partition.in(), merge.out());
                })
        );
    }

}
