package io.tapdata.postgres;

import io.tapdata.entity.schema.TapField;
import io.tapdata.entity.schema.TapTable;
import io.tapdata.entity.schema.value.DateTime;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * make sql
 *
 * @author Jarad
 * @date 2022/4/29
 */
public class SqlBuilder {

    /**
     * combine column definition for creating table
     *
     * @param tapTable Table Object
     * @return substring of SQL
     */
    public static String buildColumnDefinition(TapTable tapTable) {
        LinkedHashMap<String, TapField> nameFieldMap = tapTable.getNameFieldMap();
        StringBuilder builder = new StringBuilder();
        nameFieldMap.keySet().forEach(columnName -> {
            TapField tapField = nameFieldMap.get(columnName);
            if (tapField.getOriginType() == null)
                return;
            builder.append(tapField.getName()).append(' ').append(tapField.getOriginType()).append(' ');
            if (tapField.getNullable() != null && !tapField.getNullable()) {
                builder.append("NOT NULL").append(' ');
            } else {
                builder.append("NULL").append(' ');
            }
            if (tapField.getDefaultValue() != null) {
                builder.append("DEFAULT").append(' ').append(tapField.getDefaultValue()).append(' ');
            }
            builder.append(',');
        });
        builder.delete(builder.length() - 1, builder.length());
        return builder.toString();
    }

    /**
     * combine columns for inserting records
     *
     * @param tapTable Table Object
     * @return insert SQL
     */
    public static String buildPrepareInsertSQL(TapTable tapTable) {
        LinkedHashMap<String, TapField> nameFieldMap = tapTable.getNameFieldMap();
        StringBuilder stringBuilder = new StringBuilder();
        long fieldCount = nameFieldMap.keySet().stream().filter(v -> null != nameFieldMap.get(v).getOriginType()).count();
        for (int i = 0; i < fieldCount; i++) {
            stringBuilder.append("?, ");
        }
        stringBuilder.delete(stringBuilder.length() - 2, stringBuilder.length());
        return "INSERT INTO " + tapTable.getName() + " VALUES (" + stringBuilder + ")";
    }

    public static void addBatchInsertRecord(TapTable tapTable, Map<String, Object> insertRecord, PreparedStatement preparedStatement) throws SQLException {
        preparedStatement.clearParameters();
        LinkedHashMap<String, TapField> nameFieldMap = tapTable.getNameFieldMap();
        int pos = 1;
        for (String columnName : nameFieldMap.keySet()) {
            TapField tapField = nameFieldMap.get(columnName);
            Object tapValue = insertRecord.get(columnName);
            if (tapField.getOriginType() == null)
                continue;
            if (tapValue == null) {
                if (tapField.getNullable() != null && !tapField.getNullable()) {
                    preparedStatement.setObject(pos, tapField.getDefaultValue());
                } else {
                    preparedStatement.setObject(pos, null);
                }
            } else {
                preparedStatement.setObject(pos, getFieldOriginValue(tapValue));
            }
            pos += 1;
        }
        preparedStatement.addBatch();
    }

    public static String buildKeyAndValue(Map<String, Object> record, String splitSymbol) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Object> entry : record.entrySet()) {
            String fieldName = entry.getKey();
            builder.append(fieldName).append("=");
            if (!(entry.getValue() instanceof Number))
                builder.append("'");

            builder.append(getFieldOriginValue(entry.getValue()));

            if (!(entry.getValue() instanceof Number))
                builder.append("'");

            builder.append(splitSymbol).append(" ");
        }
        builder.delete(builder.length() - splitSymbol.length() - 1, builder.length());
        return builder.toString();
    }

    public String buildInsertValues(TapTable tapTable, Map<String, Object> record) {
        LinkedHashMap<String, TapField> nameFieldMap = tapTable.getNameFieldMap();
        StringBuilder builder = new StringBuilder();
        for (String columnName : nameFieldMap.keySet()) {
            TapField tapField = nameFieldMap.get(columnName);
            Object tapValue = record.get(columnName);
            if (tapField.getOriginType() == null)
                continue;
            if (tapValue == null) {
                if (tapField.getNullable() != null && !tapField.getNullable()) {
                    builder.append("'").append(tapField.getDefaultValue()).append("'").append(',');
                } else {
                    builder.append("null").append(',');
                }
            } else {
                builder.append("'").append(getFieldOriginValue(tapValue)).append("'").append(',');
            }
        }
        builder.delete(builder.length() - 1, builder.length());
        return builder.toString();
    }

    private static final SimpleDateFormat tapDateTimeFormat = new SimpleDateFormat();

    private static String formatTapDateTime(DateTime dateTime, String pattern) {
        if (dateTime.getTimeZone() != null) dateTime.setTimeZone(dateTime.getTimeZone());
        tapDateTimeFormat.applyPattern(pattern);
        return tapDateTimeFormat.format(new Date(dateTime.getSeconds() * 1000L));
    }

    private static String formatTapDateTime(Date date, String pattern) {
        tapDateTimeFormat.applyPattern(pattern);
        return tapDateTimeFormat.format(date);
    }

    private static Object getFieldOriginValue(Object tapValue) {
        Object result = tapValue;
        if (tapValue instanceof DateTime) {
            result = formatTapDateTime((DateTime) tapValue, "yyyy-MM-dd HH:mm:ss");
        } else if (tapValue instanceof Date) {
            result = formatTapDateTime((Date) tapValue, "yyyy-MM-dd HH:mm:ss");
        }
        return result;
    }
}