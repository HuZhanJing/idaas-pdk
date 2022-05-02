package io.tapdata.pdk.core.workflow.engine.driver;

import io.tapdata.entity.codec.filter.TapCodecFilterManager;
import io.tapdata.entity.conversion.TargetTypesGenerator;
import io.tapdata.entity.event.TapBaseEvent;
import io.tapdata.entity.event.TapEvent;
import io.tapdata.entity.event.control.ControlEvent;
import io.tapdata.entity.event.control.PatrolEvent;
import io.tapdata.entity.event.control.TapForerunnerEvent;
import io.tapdata.entity.event.ddl.TapDDLEvent;
import io.tapdata.entity.event.ddl.table.*;
import io.tapdata.entity.event.dml.*;
import io.tapdata.entity.event.dml.TapDeleteRecordEvent;
import io.tapdata.entity.event.dml.TapRecordEvent;
import io.tapdata.entity.result.ResultItem;
import io.tapdata.entity.result.TapResult;
import io.tapdata.entity.schema.TapField;
import io.tapdata.entity.schema.TapTable;
import io.tapdata.entity.utils.ClassFactory;
import io.tapdata.entity.utils.InstanceFactory;
import io.tapdata.entity.utils.cache.CacheFactory;
import io.tapdata.entity.utils.cache.KVMap;
import io.tapdata.pdk.apis.functions.connector.target.*;
import io.tapdata.entity.logger.TapLogger;
import io.tapdata.pdk.apis.pretty.ClassHandlers;
import io.tapdata.pdk.core.api.TargetNode;
import io.tapdata.pdk.core.error.CoreException;
import io.tapdata.pdk.core.error.ErrorCodes;
import io.tapdata.pdk.core.monitor.PDKInvocationMonitor;
import io.tapdata.pdk.core.monitor.PDKMethod;
import io.tapdata.pdk.core.utils.CommonUtils;
import io.tapdata.pdk.core.utils.LoggerUtils;
import io.tapdata.pdk.core.utils.queue.ListHandler;
import io.tapdata.pdk.core.workflow.engine.JobOptions;
import io.tapdata.pdk.core.workflow.engine.driver.task.TaskManager;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.tapdata.entity.simplify.TapSimplify.table;

public class TargetNodeDriver extends Driver implements ListHandler<List<TapEvent>> {
    private static final String TAG = TargetNodeDriver.class.getSimpleName();

    private TargetNode targetNode;

    private List<String> actionsBeforeStart;

    private AtomicBoolean started = new AtomicBoolean(false);

    private AtomicBoolean firstNonControlReceived = new AtomicBoolean(false);
    private KVMap<TapTable> tableKVMap;
    private ClassHandlers classHandlers = new ClassHandlers();
    public TargetNodeDriver() {
        classHandlers.register(TapCreateTableEvent.class, this::handleCreateTableEvent);
        classHandlers.register(TapAlterTableEvent.class, this::handleAlterTableEvent);
        classHandlers.register(TapClearTableEvent.class, this::handleClearTableEvent);
        classHandlers.register(TapDropTableEvent.class, this::handleDropTableEvent);

        classHandlers.register(TapInsertRecordEvent.class, this::filterInsertEvent);
        classHandlers.register(TapUpdateRecordEvent.class, this::filterUpdateEvent);
        classHandlers.register(TapDeleteRecordEvent.class, this::filterDeleteEvent);
    }

    private void handleCreateTableEvent(TapCreateTableEvent createTableEvent) {
        PDKInvocationMonitor pdkInvocationMonitor = PDKInvocationMonitor.getInstance();
        CreateTableFunction createTableFunction = targetNode.getConnectorFunctions().getCreateTableFunction();
        if(createTableFunction != null) {
            TapLogger.debug(TAG, "Create table {} before start. {}", createTableEvent.getTable(), LoggerUtils.targetNodeMessage(targetNode));


            pdkInvocationMonitor.invokePDKMethod(PDKMethod.TARGET_CREATE_TABLE, () -> {
                createTableFunction.createTable(getTargetNode().getConnectorContext(), createTableEvent);
            }, "Create table " + LoggerUtils.targetNodeMessage(targetNode), TAG);
        }
    }

    private void handleAlterTableEvent(TapAlterTableEvent alterTableEvent) {
        PDKInvocationMonitor pdkInvocationMonitor = PDKInvocationMonitor.getInstance();
        AlterTableFunction alterTableFunction = targetNode.getConnectorFunctions().getAlterTableFunction();
        if(alterTableFunction != null) {
            TapLogger.debug(TAG, "Alter table {} before start. {}", alterTableEvent.getTableId(), LoggerUtils.targetNodeMessage(targetNode));
            pdkInvocationMonitor.invokePDKMethod(PDKMethod.TARGET_ALTER_TABLE, () -> {
                alterTableFunction.alterTable(getTargetNode().getConnectorContext(), alterTableEvent);
            }, "Alter table " + LoggerUtils.targetNodeMessage(targetNode), TAG);
        }
    }

    private void handleClearTableEvent(TapClearTableEvent clearTableEvent) {
        PDKInvocationMonitor pdkInvocationMonitor = PDKInvocationMonitor.getInstance();
        ClearTableFunction clearTableFunction = targetNode.getConnectorFunctions().getClearTableFunction();
        if(clearTableFunction != null) {
            TapLogger.debug(TAG, "Clear table {} before start. {}", clearTableEvent.getTableId(), LoggerUtils.targetNodeMessage(targetNode));
            pdkInvocationMonitor.invokePDKMethod(PDKMethod.TARGET_CLEAR_TABLE, () -> {
                clearTableFunction.clearTable(getTargetNode().getConnectorContext(), clearTableEvent);
            }, "Clear table " + LoggerUtils.targetNodeMessage(targetNode), TAG);
        }
    }

    private void handleDropTableEvent(TapDropTableEvent dropTableEvent) {
        PDKInvocationMonitor pdkInvocationMonitor = PDKInvocationMonitor.getInstance();
        DropTableFunction dropTableFunction = targetNode.getConnectorFunctions().getDropTableFunction();
        if(dropTableFunction != null) {
            TapLogger.debug(TAG, "Drop table {} before start. {}", dropTableEvent.getTableId(), LoggerUtils.targetNodeMessage(targetNode));
            pdkInvocationMonitor.invokePDKMethod(PDKMethod.TARGET_DROP_TABLE, () -> {
                dropTableFunction.dropTable(getTargetNode().getConnectorContext(), dropTableEvent);
                //clear the index and fields
//                TapTable table = targetNode.getConnectorContext().getTable();
//                if(table != null) {
//                    table.setIndexList(null);
//                    table.setNameFieldMap(null);
//                }
            }, "Drop table " + LoggerUtils.targetNodeMessage(targetNode), TAG);
        }
    }


    @Override
    public void execute(List<List<TapEvent>> list) throws Throwable {
//        if(started.compareAndSet(false, true)) {
//            replaceTableFromDiscovered();
//        }

//        if(connected.compareAndSet(false, true)) {
//            ConnectFunction connectFunction = targetNode.getTargetFunctions().getConnectFunction();
//            if(connectFunction != null) {
//                pdkInvocationMonitor.invokePDKMethod(PDKMethod.TARGET_CONNECT, () -> {
//                    connectFunction.connect(targetNode.getConnectorContext());
//                }, "connect " + LoggerUtils.targetNodeMessage(targetNode), logger);
//            }
//        }

        List<TapRecordEvent> recordEvents = new ArrayList<>();
        List<ControlEvent> controlEvents = new ArrayList<>();
        for(List<TapEvent> events : list) {
//            targetNode.pullAllExternalEvents(tapEvent -> events.add(tapEvent));
            for (TapEvent event : events) {
                if(event instanceof TapDDLEvent) {
                    //force to handle DML before handle DDL.
                    handleRecordEvents(recordEvents);
                    handleControlEvent(controlEvents);
                    //handle ddl events
                    handleDDLEvent((TapDDLEvent) event);
                } else if(event instanceof TapRecordEvent) {
                    recordEvents.add(filterEvent((TapRecordEvent) event));
                } else if(event instanceof ControlEvent) {
                    if(event instanceof PatrolEvent) {
                        PatrolEvent patrolEvent = (PatrolEvent) event;
                        if(patrolEvent.applyState(targetNode.getAssociateId(), PatrolEvent.STATE_ENTER)) {
                            if(patrolEvent.getPatrolListener() != null) {
                                CommonUtils.ignoreAnyError(() -> patrolEvent.getPatrolListener().patrol(targetNode.getAssociateId(), PatrolEvent.STATE_ENTER), TAG);
                            }
                        }
                    }
                    controlEvents.add((ControlEvent) event);
                }
            }
        }
        handleRecordEvents(recordEvents);
        handleControlEvent(controlEvents);
    }

    private void tableInitialCheck(TapTable incomingTable) {
        TapTable targetTable = targetNode.getConnectorContext().getTable();
        LinkedHashMap<String, TapField> targetFieldMap = targetTable.getNameFieldMap();
        if(targetFieldMap == null || targetFieldMap.isEmpty()) {
            return;
        }

        LinkedHashMap<String, TapField> incomingTableFieldMap = incomingTable.getNameFieldMap();
        if(incomingTableFieldMap == null) {
            return;
        }

        StringBuilder builder = new StringBuilder();
        boolean somethingWrong = false;
        for(Map.Entry<String, TapField> entry : incomingTableFieldMap.entrySet()) {
            TapField targetTapField = targetFieldMap.get(entry.getKey());
            if(targetTapField == null) {
                builder.append("field ").append(entry.getKey()).append(" not found; ");
                if(!somethingWrong) somethingWrong = true;
            } else {
                if(targetTapField.getTapType() != null && entry.getValue().getTapType() != null) {
                    if(!targetTapField.getTapType().getClass().equals(entry.getValue().getTapType().getClass())) {
                        builder.append("field ").append(entry.getKey()).append(" tapType doesn't match, source ").append(entry.getValue().getClass()).append(" expect ").append(targetTapField.getTapType().getClass()).append("; ");
                        if(!somethingWrong) somethingWrong = true;
                    }
                } else {
                    builder.append("field ").append(entry.getKey()).append(" tapType doesn't match (null), source ").append(entry.getValue()).append(" expect ").append(targetTapField.getTapType()).append("; ");
                    if(!somethingWrong) somethingWrong = true;
                }
            }
        }
        if(somethingWrong) {
            TapLogger.warn(TAG, "Verify table fields failed, {}, {}", builder.toString(), LoggerUtils.targetNodeMessage(targetNode));
        }
    }

    private void handleActionsBeforeStart(TapTable table) {
        configTable(table);

        if(actionsBeforeStart != null) {
            for(String action : actionsBeforeStart) {
                switch (action) {
                    case JobOptions.ACTION_DROP_TABLE:
                        final TapDropTableEvent dropTableEvent = newTableEvent(TapDropTableEvent.class, table.getId());
                        if(dropTableEvent != null)
                            classHandlers.handle(dropTableEvent);
                        break;
                    case JobOptions.ACTION_CLEAR_TABLE:
                        final TapClearTableEvent clearTableEvent = newTableEvent(TapClearTableEvent.class, table.getId());
                        if(clearTableEvent != null)
                            classHandlers.handle(clearTableEvent);
                        break;
                    case JobOptions.ACTION_CREATE_TABLE:
                        final TapCreateTableEvent createTableEvent = newTableEvent(TapCreateTableEvent.class, table.getId());
                        if(createTableEvent != null)
                            classHandlers.handle(createTableEvent);
                        break;
                    case JobOptions.ACTION_INDEX_PRIMARY:
                        break;
                    default:
                        TapLogger.error(TAG, "Action {} is unknown before start, {}", action, LoggerUtils.targetNodeMessage(targetNode));
                        break;
                }
            }
        }
    }

    private void configTable(TapTable sourceTable) {
        String nodeTable = targetNode.getConnectorContext().getTable();
        List<String> nodeTables = targetNode.getConnectorContext().getTables();
//        TapTable targetTable = table(sourceTable.getName(), sourceTable.getId());
        //Convert source table to target target by calculate the dataType of target database.
        TargetTypesGenerator targetTypesGenerator = ClassFactory.create(TargetTypesGenerator.class);
        LinkedHashMap<String, TapField> nameFieldMap = null;
        if (targetTypesGenerator != null) {
            TapResult<LinkedHashMap<String, TapField>> tapResult = targetTypesGenerator.convert(sourceTable.getNameFieldMap(), targetNode.getTapNodeInfo().getTapNodeSpecification().getDataTypesMap(), targetNode.getCodecsFilterManager());
            if(tapResult.isSuccessfully()) {
                nameFieldMap = tapResult.getData();
                targetTable.setNameFieldMap(nameFieldMap);

                List<ResultItem> resultItems = tapResult.getResultItems();
                if(resultItems != null && !resultItems.isEmpty()) {
                    for(ResultItem resultItem : resultItems) {
                        TapLogger.warn(TAG, resultItem.getItem() + ": " + resultItem.getInformation());
                    }
                }
            } else {
                TapLogger.error(TAG, "TargetTypesGenerator convert failed, {} {}", tapResult.getResultItems(), LoggerUtils.targetNodeMessage(targetNode));
            }
        } else {
            TapLogger.error(TAG, "TargetTypesGenerator is not initialized, {}", LoggerUtils.targetNodeMessage(targetNode));
        }

    }

//    private void replaceTableFromDiscovered() {
//        targetNode.getConnector().discoverSchema(targetNode.getConnectorContext(), (tables) -> {
//            if(tables != null) {
//                for(TapTable table : tables) {
//                    if(table != null) {
//                        TapTable targetTable = targetNode.getConnectorContext().getTable();
//                        if(targetTable != null && targetTable.getName() != null && targetTable.getName().equals(table.getName())) {
//                            targetNode.getConnectorContext().setTable(table);
//                            break;
//                        }
//                    }
//                }
//            }
//        });
//    }

    private void handleDDLEvent(TapDDLEvent event) {
        classHandlers.handle(event);
    }

    private void handleRecordEvents(List<TapRecordEvent> recordEvents) {
        if(recordEvents.isEmpty())
            return;
        PDKInvocationMonitor pdkInvocationMonitor = PDKInvocationMonitor.getInstance();
        WriteRecordFunction insertRecordFunction = targetNode.getConnectorFunctions().getWriteRecordFunction();
        if(insertRecordFunction != null) {
            TapLogger.debug(TAG, "Handled {} of record events, {}", recordEvents.size(), LoggerUtils.targetNodeMessage(targetNode));
            pdkInvocationMonitor.invokePDKMethod(PDKMethod.TARGET_WRITE_RECORD, () -> {
                insertRecordFunction.writeRecord(targetNode.getConnectorContext(), recordEvents, (event) -> {
                    TapLogger.debug(TAG, "Handled {} of record events, {}", recordEvents.size(), LoggerUtils.targetNodeMessage(targetNode));
                });
            }, "insert " + LoggerUtils.targetNodeMessage(targetNode), TAG);
        }
        recordEvents.clear();
    }

    private void handleControlEvent(List<ControlEvent> events) {
        if(events.isEmpty())
            return;
        PDKInvocationMonitor pdkInvocationMonitor = PDKInvocationMonitor.getInstance();
        ControlFunction controlFunction = targetNode.getConnectorFunctions().getControlFunction();

        TapLogger.debug(TAG, "Handled {} of control events, {}", events.size(), LoggerUtils.targetNodeMessage(targetNode));
        for(ControlEvent controlEvent : events) {
            if(controlFunction != null) {
                pdkInvocationMonitor.invokePDKMethod(PDKMethod.CONTROL, () -> {
                    controlFunction.control(targetNode.getConnectorContext(), controlEvent);
                }, "control event " + LoggerUtils.targetNodeMessage(targetNode), TAG);
            }

            if(controlEvent instanceof PatrolEvent) {
                PatrolEvent patrolEvent = (PatrolEvent) controlEvent;
                if(patrolEvent.applyState(targetNode.getAssociateId(), PatrolEvent.STATE_LEAVE)) {
                    if(patrolEvent.getPatrolListener() != null) {
                        CommonUtils.ignoreAnyError(() -> patrolEvent.getPatrolListener().patrol(targetNode.getAssociateId(), PatrolEvent.STATE_LEAVE), TAG);
                    }
                }
            }
            if(controlEvent instanceof TapForerunnerEvent) {
                TapForerunnerEvent forerunnerEvent = (TapForerunnerEvent) controlEvent;
                handleForerunnerEvent(forerunnerEvent);
            }
        }
        events.clear();
    }

    private void handleForerunnerEvent(TapForerunnerEvent forerunnerEvent) {
        if(!firstNonControlReceived.get()) {
            firstNonControlReceived.set(true);
            if(targetNode.getTasks() != null) {
                taskManager = new TaskManager();
                taskManager.init(targetNode.getTasks());
            }
            //
            tableKVMap = InstanceFactory.instance(CacheFactory.class).getOrCreateKVMap(targetNode.getAssociateId());

            String nodeTable = targetNode.getConnectorContext().getTable();
            List<String> nodeTables = targetNode.getConnectorContext().getTables();
        }
        TapTable table = forerunnerEvent.getTable();//tableKVMap.get(((TapBaseEvent) event).tableMapKey());
        if(table == null)
            throw new CoreException(ErrorCodes.TARGET_TABLE_NOT_FOUND_IN_TAPEVENT, "Table doesn't be found in TapEvent " + forerunnerEvent);

        handleActionsBeforeStart(table);

        tableInitialCheck(table);
    }

    private <T extends TapTableEvent> T newTableEvent(Class<T> tableEventClass, String tableId) {
        try {
            T t = tableEventClass.getConstructor().newInstance();
            t.setTableId(tableId);
            t.setTime(System.currentTimeMillis());
//            t.setPdkId(targetNode.getTapNodeInfo().getTapNodeSpecification().getId());
//            t.setPdkGroup(targetNode.getTapNodeInfo().getTapNodeSpecification().getGroup());
//            t.setPdkVersion(targetNode.getTapNodeInfo().getTapNodeSpecification().getVersion());
            return t;
        } catch (Throwable e) {
            e.printStackTrace();
            TapLogger.error(TAG, "Create table event {} failed, {}", tableEventClass, e.getMessage());
        }
        return null;
    }

    public TargetNode getTargetNode() {
        return targetNode;
    }

    public void setTargetNode(TargetNode targetNode) {
        this.targetNode = targetNode;
    }

    private TapRecordEvent filterEvent(TapRecordEvent recordEvent) {
        classHandlers.handle(recordEvent);
//        if(recordEvent instanceof TapInsertRecordEvent) {
//            filterInsertEvent((TapInsertRecordEvent) recordEvent);
//        } else if(recordEvent instanceof TapUpdateRecordEvent) {
//            filterUpdateEvent((TapUpdateRecordEvent) recordEvent);
//        } else if(recordEvent instanceof TapDeleteRecordEvent) {
//            filterDeleteEvent((TapDeleteRecordEvent) recordEvent);
//        }
        return recordEvent;
    }

    private List<TapEvent> filterEvents(List<TapEvent> events) {
        for(TapEvent tapEvent : events) {
            classHandlers.handle(tapEvent);
//            if(tapEvent instanceof TapInsertRecordEvent) {
//                filterInsertEvent((TapInsertRecordEvent) tapEvent);
//            } else if(tapEvent instanceof TapUpdateRecordEvent) {
//                filterUpdateEvent((TapUpdateRecordEvent) tapEvent);
//            } else if(tapEvent instanceof TapDeleteRecordEvent) {
//                filterDeleteEvent((TapDeleteRecordEvent) tapEvent);
//            }
        }
        return events;
    }

    private TapDeleteRecordEvent filterDeleteEvent(TapDeleteRecordEvent deleteDMLEvent) {
        TapCodecFilterManager codecFilterManager = targetNode.getCodecsFilterManager();
        codecFilterManager.transformFromTapValueMap(deleteDMLEvent.getBefore());
        return deleteDMLEvent;
    }

    private TapUpdateRecordEvent filterUpdateEvent(TapUpdateRecordEvent updateDMLEvent) {
        TapCodecFilterManager codecFilterManager = targetNode.getCodecsFilterManager();
        codecFilterManager.transformFromTapValueMap(updateDMLEvent.getAfter());
        codecFilterManager.transformFromTapValueMap(updateDMLEvent.getBefore());
        return updateDMLEvent;
    }

    private TapInsertRecordEvent filterInsertEvent(TapInsertRecordEvent insertDMLEvent) {
        TapCodecFilterManager codecFilterManager = targetNode.getCodecsFilterManager();
        codecFilterManager.transformFromTapValueMap(insertDMLEvent.getAfter());
        return insertDMLEvent;
    }

    public void setActionsBeforeStart(List<String> actionsBeforeStart) {
        this.actionsBeforeStart = actionsBeforeStart;
    }
}
