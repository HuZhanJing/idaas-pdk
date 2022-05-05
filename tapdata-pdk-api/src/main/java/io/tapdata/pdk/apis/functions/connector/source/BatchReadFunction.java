package io.tapdata.pdk.apis.functions.connector.source;

import io.tapdata.entity.event.TapEvent;
import io.tapdata.entity.schema.TapTable;
import io.tapdata.pdk.apis.context.TapConnectorContext;
import io.tapdata.pdk.apis.entity.TapTableList;
import io.tapdata.pdk.apis.functions.connector.TapFunction;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public interface BatchReadFunction extends TapFunction {
    /**
     * @param connectorContext the node context in a DAG
     * @param table the table to batch read from
     * @param offsetState if null, means start from very beginning, otherwise is the start point for batch reading.
     *                    type can be any that comfortable for saving offset state.
     * @param eventBatchSize the batch size for the max record list size when consumer#accept a batch
     * @param eventsOffsetConsumer accept the records and offsetState.
     */
    void batchRead(TapConnectorContext connectorContext, TapTable table, String offsetState, int eventBatchSize, BiConsumer<List<TapEvent>, String> eventsOffsetConsumer) throws Throwable;
}


