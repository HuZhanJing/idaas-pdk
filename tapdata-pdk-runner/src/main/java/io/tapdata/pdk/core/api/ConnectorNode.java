package io.tapdata.pdk.core.api;

import io.tapdata.entity.codec.TapCodecRegistry;
import io.tapdata.entity.codec.filter.TapCodecFilterManager;
import io.tapdata.pdk.apis.TapConnector;
import io.tapdata.pdk.apis.context.TapConnectorContext;
import io.tapdata.pdk.apis.functions.ConnectorFunctions;

public class ConnectorNode extends Node {
    private static final String TAG = ConnectorNode.class.getSimpleName();
    TapConnector connector;
    TapCodecRegistry codecsRegistry;
    TapConnectorContext connectorContext;

    ConnectorFunctions connectorFunctions;
    TapCodecFilterManager codecsFilterManager;

//    Queue<TapEvent> externalEvents;

    public void init(TapConnector tapNode, TapCodecRegistry codecsRegistry, ConnectorFunctions connectorFunctions) {
        connector = tapNode;
        this.codecsRegistry = codecsRegistry;
        this.connectorFunctions = connectorFunctions;
        codecsFilterManager = new TapCodecFilterManager(this.codecsRegistry);
//        externalEvents = new ConcurrentLinkedQueue<>();
    }

    public void init(TapConnector tapNode) {
        init(tapNode, new TapCodecRegistry(), new ConnectorFunctions());
    }

//    public void offerExternalEvent(TapEvent tapEvent) {
//        if(externalEvents != null) {
//            if(tapEvent instanceof PatrolEvent) {
//                PatrolEvent patrolEvent = (PatrolEvent) tapEvent;
//                if(patrolEvent.applyState(getAssociateId(), PatrolEvent.STATE_ENTER)) {
//                    if(patrolEvent.getPatrolListener() != null) {
//                        CommonUtils.ignoreAnyError(() -> patrolEvent.getPatrolListener().patrol(getAssociateId(), PatrolEvent.STATE_ENTER), TAG);
//                    }
//                }
//            }
//            externalEvents.offer(tapEvent);
//        }
//    }

//    public List<TapEvent> pullAllExternalEventsInList(Consumer<TapEvent> consumer) {
//        if(externalEvents != null) {
//            if(externalEvents.isEmpty()) return null;
//
//            List<TapEvent> events = new ArrayList<>();
//            TapEvent tapEvent;
//            while((tapEvent = externalEvents.poll()) != null) {
//                if(consumer != null)
//                    consumer.accept(tapEvent);
//                events.add(tapEvent);
//            }
//            return events;
//        }
//        return null;
//    }

//    public void pullAllExternalEvents(Consumer<TapEvent> consumer) {
//        if(externalEvents != null) {
//            TapEvent tapEvent;
//            while((tapEvent = externalEvents.poll()) != null) {
//                consumer.accept(tapEvent);
//            }
//        }
//    }

    public TapCodecRegistry getCodecsRegistry() {
        return codecsRegistry;
    }

    public void registerCapabilities() {
        connector.registerCapabilities(connectorFunctions, codecsRegistry);
    }

    public TapConnectorContext getConnectorContext() {
        return connectorContext;
    }

    public TapConnector getConnector() {
        return connector;
    }

    public ConnectorFunctions getConnectorFunctions() {
        return connectorFunctions;
    }

    public TapCodecFilterManager getCodecsFilterManager() {
        return codecsFilterManager;
    }
}
