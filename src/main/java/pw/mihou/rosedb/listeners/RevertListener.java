package pw.mihou.rosedb.listeners;

import org.java_websocket.WebSocket;
import org.json.JSONObject;
import pw.mihou.rosedb.RoseDB;
import pw.mihou.rosedb.connections.RoseServer;
import pw.mihou.rosedb.enums.Listening;
import pw.mihou.rosedb.io.RoseQuery;
import pw.mihou.rosedb.io.entities.QueryRequest;
import pw.mihou.rosedb.manager.RoseDatabase;
import pw.mihou.rosedb.manager.entities.RoseListener;
import pw.mihou.rosedb.utility.Pair;

public class RevertListener implements RoseListener {

    @Override
    public Listening type() {
        return Listening.REVERT;
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
        if(RoseDB.versioning)
            if(request.identifier == null || request.database == null || request.collection == null){
                RoseServer.reply(context, "Missing parameters either:  [identifier], [database], [collection]", unique, -1);
            } else {
                Pair<Boolean, String> response = RoseDatabase.getDatabase(request.database)
                        .getCollection(request.collection).revert(request.identifier);

                if(response.getLeft())
                    RoseServer.reply(context, response.getRight(), unique, 1);
                else
                    RoseServer.reply(context, "There are no backup versions for the requested item.", unique, 0);
            }
        else
            RoseServer.reply(context, "This method is not supported by the server.", unique, 0);
    }
}
