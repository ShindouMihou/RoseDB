package pw.mihou.rosedb.listeners;

import io.javalin.websocket.WsContext;
import org.json.JSONObject;
import pw.mihou.rosedb.connections.RoseServer;
import pw.mihou.rosedb.enums.Listening;
import pw.mihou.rosedb.io.RoseQuery;
import pw.mihou.rosedb.io.entities.QueryRequest;
import pw.mihou.rosedb.manager.entities.RoseListener;

public class RequestListener implements RoseListener {

    @Override
    public Listening type() {
        return Listening.GET;
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
        if(request.identifier == null || request.database == null || request.collection == null)
            RoseServer.reply(context, "Missing parameters either: [identifier], [database], [collection]", unique, -1);
        else
            RoseServer.getDatabase(request.database).getCollection(request.collection)
            .get(request.identifier).ifPresentOrElse(s -> RoseServer.reply(context, s, unique, 1), () ->
                    RoseServer.reply(context, "No results for " + request.identifier, unique, 0));
    }
}
