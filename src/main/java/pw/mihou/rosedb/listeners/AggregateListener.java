package pw.mihou.rosedb.listeners;

import org.java_websocket.WebSocket;
import org.json.JSONObject;
import pw.mihou.rosedb.connections.RoseServer;
import pw.mihou.rosedb.enums.Levels;
import pw.mihou.rosedb.enums.Listening;
import pw.mihou.rosedb.io.RoseQuery;
import pw.mihou.rosedb.io.entities.QueryRequest;
import pw.mihou.rosedb.manager.RoseCollections;
import pw.mihou.rosedb.manager.RoseDatabase;
import pw.mihou.rosedb.manager.entities.RoseListener;

import java.util.stream.Collectors;

public class AggregateListener implements RoseListener {
    @Override
    public Listening type() {
        return Listening.AGGREGATE;
    }

    @Override
    public void execute(QueryRequest request, WebSocket context, String unique) {
        handle(request, context, unique);
    }

    @Override
    public void execute(JSONObject request, WebSocket context, String unique) {
        handle(RoseQuery.parse(request), context, unique);
    }

    private void handle(QueryRequest request, WebSocket context, String unique){
        if(request.database == null && request.collection == null || request.database == null && request.collection != null){
            RoseServer.reply(context, "Missing parameters either: [database], [collection]", unique, -1);
        } else {
            if(request.collection == null){
                RoseServer.reply(context, new JSONObject().put(request.database,
                        RoseDatabase.getDatabase(request.database).getCollections().stream()
                                .collect(Collectors.toMap(collections -> collections.collection,
                                        RoseCollections::getData))), unique, 1);
            } else {
                RoseServer.reply(context, new JSONObject().put(request.collection,
                        RoseDatabase.getDatabase(request.database).getCollection(request.collection).getData()),
                        unique, 1);
            }
        }
    }
}
