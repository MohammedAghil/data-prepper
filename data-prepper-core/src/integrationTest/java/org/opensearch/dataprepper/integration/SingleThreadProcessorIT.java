/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */
package org.opensearch.dataprepper.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.opensearch.dataprepper.model.event.Event;
import org.opensearch.dataprepper.model.event.JacksonEvent;
import org.opensearch.dataprepper.model.record.Record;
import org.opensearch.dataprepper.plugins.InMemorySinkAccessor;
import org.opensearch.dataprepper.plugins.InMemorySourceAccessor;
import org.opensearch.dataprepper.plugins.SingleThreadTestProcessor;
import org.opensearch.dataprepper.test.framework.DataPrepperTestRunner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

/**
 * Integration tests to verify that events are processed exactly once
 * when using multiple workers with a @SingleThread-annotated processor.
 */
class SingleThreadProcessorIT {
    private static final String IN_MEMORY_IDENTIFIER = "SingleThreadProcessorIT";
    private static final String PIPELINE_CONFIG = "single-thread-processor.yaml";
    private static final int BATCH_SIZE = 5;
    private static final int TOTAL_EVENTS = 100;
    private static final int WAIT_TIMEOUT_SECONDS = 10;

    private DataPrepperTestRunner testRunner;
    private InMemorySourceAccessor sourceAccessor;
    private InMemorySinkAccessor sinkAccessor;

    @BeforeEach
    void setUp() {
        SingleThreadTestProcessor.reset();
        initializeTestRunner();
    }

    @AfterEach
    void tearDown() {
        if (testRunner != null) {
            testRunner.stop();
        }
    }

    /**
     * Parameterized test to verify that events are processed exactly once
     */
    @ParameterizedTest(name = "{index} - {0}")
    @MethodSource("provideTestParameters")
    void test_events_processed_exactly_once(String testName, int numberOfBatches, int eventsPerBatch, int expectedTotalEvents) {
        List<List<Record<Event>>> batches = createBatches(numberOfBatches, eventsPerBatch);
        batches.forEach(batch -> sourceAccessor.submit(IN_MEMORY_IDENTIFIER, batch));

        verifyProcessingResults(expectedTotalEvents, eventsPerBatch);
    }

    private static Stream<Arguments> provideTestParameters() {
        return Stream.of(
                Arguments.of("SingleBatch", 1, TOTAL_EVENTS, TOTAL_EVENTS),
                Arguments.of("MultipleBatches", BATCH_SIZE, TOTAL_EVENTS, BATCH_SIZE * TOTAL_EVENTS)
        );
    }

    private void initializeTestRunner() {
        testRunner = DataPrepperTestRunner.builder()
                .withPipelinesDirectoryOrFile(PIPELINE_CONFIG)
                .build();
        sourceAccessor = testRunner.getInMemorySourceAccessor();
        sinkAccessor = testRunner.getInMemorySinkAccessor();
        testRunner.start();
    }

    /**
     * Verifies that all events were processed correctly.
     * @param expectedTotalEvents The expected total number of processed events
     * @param eventsPerBatch The number of events per batch
     */
    private void verifyProcessingResults(int expectedTotalEvents, int eventsPerBatch) {
        // Wait until all expected events are processed
        await().atMost(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(
                        sinkAccessor.get(IN_MEMORY_IDENTIFIER).size(),
                        equalTo(expectedTotalEvents)));

        // Retrieve processed events
        List<Record<Event>> outputRecords = sinkAccessor.get(IN_MEMORY_IDENTIFIER);
        assertThat(outputRecords.size(), equalTo(expectedTotalEvents));

        Map<String, AtomicInteger> processedEventsMap = SingleThreadTestProcessor.getProcessedEventsMap();
        verifyEventProcessing(processedEventsMap, outputRecords, expectedTotalEvents);
        verifyWorkerThreads(outputRecords);

        // Verify batch and sequence fields
        int numberOfBatches = expectedTotalEvents / eventsPerBatch;
        for (int batch = 0; batch < numberOfBatches; batch++) {
            int finalBatch = batch;
            List<Record<Event>> batchRecords = outputRecords.stream()
                    .filter(record -> record.getData().get("batch", Integer.class) == finalBatch)
                    .collect(Collectors.toList());
            assertThat(batchRecords.size(), equalTo(eventsPerBatch));
        }
    }

    /**
     * Verifies that each event was processed exactly once.
     * @param processedEventsMap Map of event IDs to their processing counts
     * @param outputRecords The processed events
     * @param expectedTotalEvents The expected total number of events
     */
    private void verifyEventProcessing(Map<String, AtomicInteger> processedEventsMap,
                                       List<Record<Event>> outputRecords,
                                       int expectedTotalEvents) {
        // Check each processed event
        for (Record<Event> record : outputRecords) {
            String id = record.getData().get("id", String.class);
            AtomicInteger count = processedEventsMap.get(id);

            // Verify each event was processed exactly once
            assertThat("Event with ID " + id + " should be processed exactly once",
                    count.get(), equalTo(1));
            // Verify the processing count in the event metadata
            assertThat("Event processing count should be 1",
                    record.getData().get("processed_count", Integer.class), equalTo(1));
        }

        // Verify all expected events were processed
        assertThat("All events should be processed",
                processedEventsMap.size(), equalTo(expectedTotalEvents));
    }

    /**
     * Verifies that multiple worker threads processed the events.
     * @param outputRecords The processed events
     */
    private void verifyWorkerThreads(List<Record<Event>> outputRecords) {
        // Get the list of unique thread names that processed the events
        Set<String> threadNames = outputRecords.stream()
                .map(Record::getData)
                .map(event -> event.get("processed_by_thread", String.class))
                .collect(Collectors.toSet());

        // Verify multiple threads were used (at least one)
        assertThat("There should be at least one worker thread",
                threadNames.size(), greaterThanOrEqualTo(1));
    }

    /**
     * Creates a list of test records.
     * @param count Number of records to create
     * @return List of records
     */
    private List<Record<Event>> createRecords(int count) {
        List<Record<Event>> records = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            records.add(new Record<>(createEvent(i)));
        }
        return records;
    }

    /**
     * Creates multiple batches of test records.
     * @param batchSize Number of batches
     * @param eventsPerBatch Number of events per batch
     * @return List of batches, each containing records
     */
    private List<List<Record<Event>>> createBatches(int batchSize, int eventsPerBatch) {
        List<List<Record<Event>>> batches = new ArrayList<>();
        for (int batch = 0; batch < batchSize; batch++) {
            int batchOffset = batch * eventsPerBatch;
            int currentBatch = batch;
            List<Record<Event>> batchRecords = createRecords(eventsPerBatch).stream()
                    .map(record -> {
                        Event event = record.getData();
                        event.put("sequence", batchOffset + event.get("sequence", Integer.class));
                        event.put("batch", currentBatch);
                        return new Record<>(event);
                    })
                    .collect(Collectors.toList());
            batches.add(batchRecords);
        }
        return batches;
    }

    /**
     * Creates a single test event with a unique ID and sequence number.
     * @param sequence The sequence number for the event
     * @return The created event
     */
    private Event createEvent(int sequence) {
        String eventId = UUID.randomUUID().toString();
        Event event = JacksonEvent.fromMessage(eventId);
        event.put("id", eventId);
        event.put("sequence", sequence);
        return event;
    }
}