package io.tapdata.base;

import io.tapdata.entity.event.dml.TapDeleteRecordEvent;
import io.tapdata.entity.event.dml.TapInsertRecordEvent;
import io.tapdata.entity.event.dml.TapRecordEvent;
import io.tapdata.entity.event.dml.TapUpdateRecordEvent;
import io.tapdata.entity.logger.TapLogger;
import io.tapdata.entity.schema.TapField;
import io.tapdata.entity.schema.TapTable;
import io.tapdata.entity.schema.type.*;
import io.tapdata.entity.schema.value.DateTime;
import io.tapdata.entity.simplify.TapSimplify;
import io.tapdata.entity.utils.*;
import io.tapdata.pdk.apis.TapConnector;
import io.tapdata.pdk.apis.context.TapConnectionContext;
import io.tapdata.pdk.apis.context.TapConnectorContext;
import io.tapdata.pdk.apis.entity.TestItem;
import io.tapdata.pdk.apis.entity.WriteListResult;
import io.tapdata.pdk.apis.utils.TypeConverter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public abstract class ConnectorBase implements TapConnector {
    private static final TypeConverter typeConverter = InstanceFactory.instance(TypeConverter.class);
    private static final SimpleDateFormat tapDateTimeFormat = new SimpleDateFormat();
    private static final String TAG = ConnectorBase.class.getSimpleName();

    public static void interval(Runnable runnable, int seconds) {
        TapSimplify.interval(runnable, seconds);
    }

    public static Long toLong(Object value) {
        return typeConverter.toLong(value);
    }

    public static Integer toInteger(Object value) {
        return typeConverter.toInteger(value);
    }

    public static Short toShort(Object value) {
        return typeConverter.toShort(value);
    }

    public static List<String> toStringArray(Object value) {
        return typeConverter.toStringArray(value);
    }

    public static String toString(Object value) {
        return typeConverter.toString(value);
    }

    public static Byte toByte(Object value) {
        return typeConverter.toByte(value);
    }

    public static Double toDouble(Object value) {
        return typeConverter.toDouble(value);
    }

    public static Float toFloat(Object value) {
        return typeConverter.toFloat(value);
    }

    public static Boolean toBoolean(Object value) {
        return typeConverter.toBoolean(value);
    }

    public static String toJson(Object obj, JsonParser.ToJsonFeature... features) {
        return TapSimplify.toJson(obj, features);
    }

    public static Object fromJson(String json) {
        return TapSimplify.fromJson(json);
    }
    public static DataMap fromJsonObject(String json) {
        return TapSimplify.fromJsonObject(json);
    }

    public static List<?> fromJsonArray(String json) {
        return TapSimplify.fromJsonArray(json);
    }
    public static <T> T fromJson(String json, Class<T> clazz) {
        return TapSimplify.fromJson(json, clazz);
    }

    public static String format(String message, Object... args) {
        return FormatUtils.format(message, args);
    }

    public static TapField field(String name, String dataType) {
        return TapSimplify.field(name, dataType);
    }

    public static TapTable table(String tableName, String id) {
        return TapSimplify.table(tableName, id);
    }

    public static TapTable table(String nameAndId) {
        return TapSimplify.table(nameAndId);
    }

    public static TapString tapString() {
        return TapSimplify.tapString();
    }

    public static TapNumber tapNumber() {
        return TapSimplify.tapNumber();
    }

    public static TapRaw tapRaw() {
        return TapSimplify.tapRaw();
    }

    public static TapArray tapArray() {
        return TapSimplify.tapArray();
    }

    public static TapMap tapMap() {
        return TapSimplify.tapMap();
    }

    public static TapYear tapYear() {
        return TapSimplify.tapYear();
    }

    public static TapDate tapDate() {
        return TapSimplify.tapDate();
    }

    public static TapBoolean tapBoolean() {
        return TapSimplify.tapBoolean();
    }

    public static TapBinary tapBinary() {
        return TapSimplify.tapBinary();
    }

    public static TapTime tapTime() {
        return TapSimplify.tapTime();
    }

    public static TapDateTime tapDateTime() {
        return TapSimplify.tapDateTime();
    }

    public static TestItem testItem(String item, int resultCode) {
        return testItem(item, resultCode, null);
    }

    public static TestItem testItem(String item, int resultCode, String information) {
        return new TestItem(item, resultCode, information);
    }

    public static Entry entry(String key, Object value) {
        return TapSimplify.entry(key, value);
    }

    public static <T> List<T> list(T... ts) {
        return TapSimplify.list(ts);
    }

    public static <T> List<T> list() {
        return TapSimplify.list();
    }

    public static Map<String, Object> map() {
        return TapSimplify.map();
    }

    public static Map<String, Object> map(Entry... entries) {
        return TapSimplify.map(entries);
    }

    public static TapInsertRecordEvent insertRecordEvent(Map<String, Object> after, String table) {
        return TapSimplify.insertRecordEvent(after, table);
    }

    public static TapDeleteRecordEvent deleteDMLEvent(Map<String, Object> before, String table) {
        return TapSimplify.deleteDMLEvent(before, table);
    }

    public static TapUpdateRecordEvent updateDMLEvent(Map<String, Object> before, Map<String, Object> after, String table) {
        return TapSimplify.updateDMLEvent(before, after, table);
    }

    public static WriteListResult<TapRecordEvent> writeListResult() {
        return new WriteListResult<>();
    }

    public static void sleep(long milliseconds) {
        TapSimplify.sleep(milliseconds);
    }

    public static String formatTapDateTime(DateTime dateTime, String pattern) {
        try {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(pattern);
            LocalDateTime localDateTime = LocalDateTime.ofInstant(dateTime.toInstant(), ZoneId.of("GMT"));
            return dateTimeFormatter.format(localDateTime);
        } catch (Throwable e) {
            e.printStackTrace();
            TapLogger.error(TAG, "Parse date time {} pattern {}, failed, {}", dateTime, pattern, e.getMessage());
        }
        return null;
    }

    public static Object convertDateTimeToDate(DateTime dateTime) {
        return TapSimplify.convertDateTimeToDate(dateTime);
    }

    public static String getStackString(Throwable throwable) {
        StringWriter sw = new StringWriter();
        try (
                PrintWriter pw = new PrintWriter(sw)
        ) {
            throwable.printStackTrace(pw);
            return sw.toString();
        }
    }

    private final AtomicBoolean isAlive = new AtomicBoolean(false);
    private final AtomicBoolean isDestroyed = new AtomicBoolean(false);

    public boolean isAlive() {
        return isAlive.get() && !Thread.currentThread().isInterrupted();
    }

    @Override
    public final void init(TapConnectionContext connectionContext) throws Throwable {
        if (isAlive.compareAndSet(false, true)) {
            onStart(connectionContext);
        }
    }

    public abstract void onStart(TapConnectionContext connectionContext) throws Throwable;

    public abstract void onDestroy(TapConnectionContext connectionContext) throws Throwable;

    public abstract void onPause(TapConnectionContext connectionContext) throws Throwable;

    @Override
    public final void destroy(TapConnectionContext connectionContext) throws Throwable {
        if (isDestroyed.compareAndSet(false, true)) {
            pause(connectionContext);
            onDestroy(connectionContext);
        }
    }

    @Override
    public void pause(TapConnectionContext connectionContext) throws Throwable {
        if (isAlive.compareAndSet(true, false)) {
            onPause(connectionContext);
        }
    }

    protected void isConnectorStarted(TapConnectionContext connectionContext, Consumer<TapConnectorContext> contextConsumer) {
        if(connectionContext instanceof TapConnectorContext) {
            if(contextConsumer != null) {
                contextConsumer.accept((TapConnectorContext) connectionContext);
            }
        }
    }
}
