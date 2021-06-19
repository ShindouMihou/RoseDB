package pw.mihou.rosedb.listeners;

import org.java_websocket.WebSocket;
import org.json.JSONObject;
import pw.mihou.rosedb.connections.RoseServer;
import pw.mihou.rosedb.enums.Listening;
import pw.mihou.rosedb.io.RoseQuery;
import pw.mihou.rosedb.io.entities.QueryRequest;
import pw.mihou.rosedb.manager.RoseDatabase;
import pw.mihou.rosedb.manager.entities.RoseListener;

public class DropListener implements RoseListener {
    @Override
    public Listening type() {
        return Listening.DROP;
    }

    @Override
    public void execute(QueryRequest request, WebSocket context, String unique) {
        handle(request, context, unique);
    }

    @Override
    public void execute(JSONObject request, WebSocket context, String unique) {
        handle(RoseQuery.parse(request), context, unique);
    }

    @SuppressWarnings("ConstantConditions")
    private void handle(QueryRequest request, WebSocket context, String unique){
        if(request.database == null && request.collection == null || request.database == null && request.collection != null){
            RoseServer.reply(context, "Missing parameters either: [database], [collection]", unique, -1);
        } else {
            if(request.collection == null){
                RoseDatabase.removeDatabase(request.database);
                RoseServer.reply(context, "Successfully deleted the database " + request.database, unique, 1);
            } else {
                RoseDatabase.getDatabase(request.database).removeCollection(request.collection);
                RoseServer.reply(context, "Successfully deleted the collection " + request.collection, unique, 1);
            }
        }
    }
}
