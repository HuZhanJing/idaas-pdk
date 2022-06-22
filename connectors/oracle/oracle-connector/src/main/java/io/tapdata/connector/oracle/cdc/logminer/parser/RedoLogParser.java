package io.tapdata.connector.oracle.cdc.logminer.parser;

import io.tapdata.connector.oracle.cdc.logminer.OracleConstant;
import io.tapdata.connector.oracle.cdc.logminer.bean.RedoLogContent;
import org.apache.commons.lang3.StringUtils;

import java.util.Map;

public interface RedoLogParser {

    Map<String, Object> parseSQL(String sql, RedoLogContent.OperationEnum operationEnum);

    /**
     * 需要解析undo的条件
     * (是更新事件，并且有undo sql) and ((开启了mongodb before配置) or (目标是oracle，并且开启了修改主键配置))
     */
    default boolean needParseUndo(String operation, String undoSql, String mongodbBefore, boolean supportUpdatePk, boolean noPrimaryKey) {
        return StringUtils.isNotBlank(undoSql) && StringUtils.equalsAnyIgnoreCase(operation, OracleConstant.REDO_LOG_OPERATION_UPDATE)
                && ("true".equals(mongodbBefore) || supportUpdatePk || noPrimaryKey);
    }

    default void close() {
        // do nothing
    }
}
