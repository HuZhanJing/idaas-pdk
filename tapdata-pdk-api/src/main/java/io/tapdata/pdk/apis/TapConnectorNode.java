package io.tapdata.pdk.apis;

import io.tapdata.entity.schema.TapTable;
import io.tapdata.pdk.apis.context.TapConnectionContext;
import io.tapdata.pdk.apis.entity.TestItem;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;

/**
 * Tapdata connector node.
 * Can be TapSource or TapTarget. Stand for a data source, not a processor.
 */
public interface TapConnectorNode extends TapNode  {
    /**
     * Return all tables in a database when tables is null.
     * Return the tables in a database specified by param tables
     *
     * @param connectionContext
     * @param tables
     * @param consumer
     */
    void discoverSchema(TapConnectionContext connectionContext, List<String> tables, Consumer<List<TapTable>> consumer) throws Throwable;

    /**
     * Test connection
     * @param databaseContext
     * @return
     */
    void connectionTest(TapConnectionContext databaseContext, Consumer<TestItem> consumer) throws Throwable;

}
