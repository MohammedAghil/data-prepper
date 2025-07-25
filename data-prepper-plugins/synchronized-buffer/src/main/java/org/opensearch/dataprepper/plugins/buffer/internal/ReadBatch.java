/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.dataprepper.plugins.buffer.internal;

import org.opensearch.dataprepper.model.record.Record;

/**
 * Tracks batches that have been read for proper checkpointing.
 */
public class ReadBatch<T extends Record<?>> {
    private final SignaledBatch<T> batch;
    private final int count;

    public ReadBatch(SignaledBatch<T> batch, int count) {
        this.batch = batch;
        this.count = count;
    }
    
    public SignaledBatch<T> getBatch() {
        return batch;
    }
    
    public int getCount() {
        return count;
    }
}
