/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.dataprepper.plugins.lambda.common.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.time.DurationMax;
import org.hibernate.validator.constraints.time.DurationMin;
import org.opensearch.dataprepper.model.types.ByteCount;

import java.time.Duration;


public class ThresholdOptions {
    public static final int DEFAULT_EVENT_COUNT = 100;
    public static final String DEFAULT_BYTE_CAPACITY = "5mb";
    public static final Duration DEFAULT_EVENT_TIMEOUT = Duration.ofSeconds(10);

    @JsonProperty("event_count")
    @Size(min = 0, max = 10000000, message = "event_count size should be between 0 and 10000000")
    @NotNull
    private int eventCount = DEFAULT_EVENT_COUNT;

    @JsonProperty("maximum_size")
    private String maximumSize = DEFAULT_BYTE_CAPACITY;

    @JsonProperty("event_collect_timeout")
    @DurationMin(seconds = 1)
    @DurationMax(seconds = 3600)
    @NotNull
    private Duration eventCollectTimeOut = DEFAULT_EVENT_TIMEOUT;

    /**
     * Read event collection duration configuration.
     * @return  event collect time out.
     */
    public Duration getEventCollectTimeOut() {
        return eventCollectTimeOut;
    }

    /**
     * Read byte capacity configuration.
     * @return maximum byte count.
     */
    public ByteCount getMaximumSize() {
        return ByteCount.parse(maximumSize);
    }

    /**
     * Read the event count configuration.
     * @return event count.
     */
    public int getEventCount() {
        return eventCount;
    }
}