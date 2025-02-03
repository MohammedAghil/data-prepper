package org.opensearch.dataprepper.plugins.buffer.zerobuffer;

import org.opensearch.dataprepper.metrics.MetricNames;
import org.opensearch.dataprepper.metrics.PluginMetrics;
import org.opensearch.dataprepper.model.annotations.DataPrepperPlugin;
import org.opensearch.dataprepper.model.annotations.DataPrepperPluginConstructor;
import org.opensearch.dataprepper.model.buffer.Buffer;
import org.opensearch.dataprepper.model.CheckpointState;
import org.opensearch.dataprepper.model.configuration.PipelineDescription;
import org.opensearch.dataprepper.model.record.Record;
import io.micrometer.core.instrument.Counter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import lombok.Getter;
import lombok.AccessLevel;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import com.google.common.annotations.VisibleForTesting;

@DataPrepperPlugin(name = "zero_buffer", pluginType = Buffer.class)
public class ZeroBuffer<T extends Record<?>> extends AbstractZeroBuffer<T> implements PipelineDescription {
    private static final Logger LOG = LoggerFactory.getLogger(ZeroBuffer.class);
    private static final String PLUGIN_NAME = "zero_buffer";
    private static final String PLUGIN_COMPONENT_ID = "ZeroBuffer";
    private final String pipelineName;
    private final PluginMetrics pluginMetrics;
    private final ThreadLocal<Collection<T>> threadLocalStore;
    @Getter(value = AccessLevel.PACKAGE)
    private final Counter writeRecordsCounter;
    @Getter(value = AccessLevel.PACKAGE)
    private final Counter readRecordsCounter;

    @DataPrepperPluginConstructor
    public ZeroBuffer(String pipelineName) {
        this.pipelineName = pipelineName;
        this.pluginMetrics = PluginMetrics.fromNames(PLUGIN_COMPONENT_ID, pipelineName);
        this.writeRecordsCounter = pluginMetrics.counter(MetricNames.RECORDS_WRITTEN);
        this.readRecordsCounter = pluginMetrics.counter(MetricNames.RECORDS_READ);
        this.threadLocalStore = new ThreadLocal<>();
    }

    @Override
    public void write(T record, int timeoutInMillis) throws TimeoutException {
        if (record == null) {
            throw new NullPointerException("The write record cannot be null");
        }

        if (threadLocalStore.get() == null) {
            threadLocalStore.set(new ArrayList<>());
        }

        threadLocalStore.get().add(record);
        writeRecordsCounter.increment();

        runAllProcessorsAndPublishToSinks();
    }

    @Override
    public void writeAll(Collection<T> records, int timeoutInMillis) throws Exception {
        if (records == null) {
            throw new NullPointerException("The write records cannot be null");
        }

        if (threadLocalStore.get() == null) {
            threadLocalStore.set(records);
        } else {
            // Add the new records to the existing records
            threadLocalStore.get().addAll(records);
        }

        writeRecordsCounter.increment(records.size() * 1.0);
        runAllProcessorsAndPublishToSinks();
    }

    @Override
    public Map.Entry<Collection<T>, CheckpointState> read(int timeoutInMillis) {
        if (threadLocalStore.get() == null) {
            threadLocalStore.set(new ArrayList<>());
        }

        Collection<T> storedRecords = threadLocalStore.get();
        CheckpointState checkpointState = new CheckpointState(0);
        if (storedRecords!= null && !storedRecords.isEmpty()) {
            checkpointState = new CheckpointState(storedRecords.size());
            threadLocalStore.remove();
            readRecordsCounter.increment(storedRecords.size() * 1.0);
        }

        return new AbstractMap.SimpleEntry<>(storedRecords, checkpointState);
    }

    @Override
    public void checkpoint(CheckpointState checkpointState) {}

    @Override
    public boolean isEmpty() {
        return (this.threadLocalStore.get() == null || this.threadLocalStore.get().isEmpty());
    }

    @Override
    public String getPipelineName() {
        return pipelineName;
    }

    @Override
    public int getNumberOfProcessWorkers() {
        return 0;
    }
}
