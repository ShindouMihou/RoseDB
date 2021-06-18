package pw.mihou.rosedb.listeners;

import io.javalin.websocket.WsContext;
import org.json.JSONObject;
import pw.mihou.rosedb.RoseDB;
import pw.mihou.rosedb.connections.RoseServer;
import pw.mihou.rosedb.enums.Listening;
import pw.mihou.rosedb.io.RoseQuery;
import pw.mihou.rosedb.io.entities.QueryRequest;
import pw.mihou.rosedb.manager.entities.RoseListener;
import pw.mihou.rosedb.utility.Pair;

public class RevertListener implements RoseListener {

    @Override
    public Listening type() {
        return Listening.REVERT;
    }

    @Override
    public void execute(QueryRequest request, WsContext context, String unique) {
        handle(request, context, unique);
    }

    @Override
    public void execute(JSONObject request, WsContext context, String unique) {
        handle(RoseQuery.parse(request), context, unique);
    }

    private void handle(QueryRequest request, WsContext context, String unique){
        if(RoseDB.versioning)
            if(request.identifier == null || request.database == null || request.collection == null){
                RoseServer.reply(context, "Missing parameters either:  [identifier], [database], [collection]", unique, -1);
            } else {
                Pair<Boolean, String> response = RoseServer.getDatabase(request.database)
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
