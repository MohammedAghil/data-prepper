/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.dataprepper.plugins.buffer.internal;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import org.opensearch.dataprepper.model.record.Record;

/**
 * Represents a batch of records with completion signaling.
 */
public class SignaledBatch<T extends Record<?>> {
    private final List<T> records;
    private final AtomicInteger remaining;
    private final AtomicInteger nextSliceIndex;
    private final CompletableFuture<Void> signal;

    public SignaledBatch(List<T> records) {
        this.records = records;
        this.remaining = new AtomicInteger(records.size());
        this.nextSliceIndex = new AtomicInteger(0);
        this.signal = new CompletableFuture<>();
    }

    public List<T> readNext(int maxSize) {
        int start = nextSliceIndex.getAndAdd(maxSize);
        if (start >= records.size()) return List.of();
        int end = Math.min(start + maxSize, records.size());
        return records.subList(start, end);
    }

    public void markProcessed(int count) {
        if (remaining.addAndGet(-count) <= 0) {
            signal.complete(null);
        }
    }
    
    public CompletableFuture<Void> getSignal() {
        return signal;
    }

    public int getRemaining() {
        return remaining.get();
    }
}
