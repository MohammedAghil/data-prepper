package org.opensearch.dataprepper.plugins.buffer.zerobuffer;

import org.opensearch.dataprepper.core.pipeline.PipelineRunner;
import org.opensearch.dataprepper.model.buffer.Buffer;
import org.opensearch.dataprepper.model.record.Record;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class AbstractZeroBuffer <T extends Record<?>> implements Buffer<T> {
    private PipelineRunner pipelineRunner;

    protected void runAllProcessorsAndPublishToSinks() {
        // TODO : Implement functionality to call the processors and sinks within the same context
        getPipelineRunner().runAllProcessorsAndPublishToSinks();
    }
}
