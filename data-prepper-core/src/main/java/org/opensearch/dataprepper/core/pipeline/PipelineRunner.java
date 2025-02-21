package org.opensearch.dataprepper.core.pipeline;

import org.opensearch.dataprepper.model.buffer.Buffer;
import org.opensearch.dataprepper.model.processor.Processor;
import java.util.List;
/**
 * Pipeline Runner interface encapsulates the functionalities of reading from buffer,
 * executing the processors and publishing to sinks to provide both synchronous and
 * asynchronous mechanism for running a pipeline.
 */
public interface PipelineRunner {
    void runAllProcessorsAndPublishToSinks();

    Pipeline getPipeline();
}
