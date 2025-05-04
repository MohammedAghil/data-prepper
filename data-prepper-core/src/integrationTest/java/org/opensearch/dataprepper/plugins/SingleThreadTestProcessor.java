/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.dataprepper.plugins;

import org.opensearch.dataprepper.model.annotations.DataPrepperPlugin;
import org.opensearch.dataprepper.model.annotations.SingleThread;
import org.opensearch.dataprepper.model.event.Event;
import org.opensearch.dataprepper.model.processor.Processor;
import org.opensearch.dataprepper.model.record.Record;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A @SingleThread Test processor that tracks the list of events processed by it
 */
@SingleThread
@DataPrepperPlugin(name = "single_thread_test", pluginType = Processor.class)
public class SingleThreadTestProcessor implements Processor<Record<Event>, Record<Event>> {
    private static final Map<String, AtomicInteger> PROCESSED_EVENTS_MAP = new ConcurrentHashMap<>();
    
    public static void reset() {
        PROCESSED_EVENTS_MAP.clear();
    }
    
    public static Map<String, AtomicInteger> getProcessedEventsMap() {
        return PROCESSED_EVENTS_MAP;
    }

    @Override
    public Collection<Record<Event>> execute(final Collection<Record<Event>> records) {
        final String threadName = Thread.currentThread().getName();
        
        for (Record<Event> record : records) {
            Event event = record.getData();
            String eventId = event.get("id", String.class);
            
            if (eventId != null) {
                AtomicInteger counter = PROCESSED_EVENTS_MAP.computeIfAbsent(eventId, id -> new AtomicInteger(0));
                int count = counter.incrementAndGet();
                
                // Add processing information to the event
                event.put("processed_count", count);
                event.put("processed_by_thread", threadName);
            }
        }
        return records;
    }

    @Override
    public void prepareForShutdown() {
    }

    @Override
    public boolean isReadyForShutdown() {
        return true;
    }

    @Override
    public void shutdown() {
    }
}
