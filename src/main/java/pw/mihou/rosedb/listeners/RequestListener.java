package pw.mihou.rosedb.listeners;

import io.javalin.websocket.WsContext;
import org.json.JSONObject;
import pw.mihou.rosedb.connections.RoseServer;
import pw.mihou.rosedb.enums.Listening;
import pw.mihou.rosedb.manager.entities.RoseListener;

public class RequestListener implements RoseListener {

    @Override
    public Listening type() {
        return Listening.GET;
    }

    @Override
    public void execute(JSONObject request, WsContext context, String unique) {
        if (request.isNull("identifier") || request.isNull("database") || request.isNull("collection")) {
            RoseServer.reply(context, "Missing parameters either: [identifier], [database], [collection]", unique, -1);
        } else {
            RoseServer.getDatabase(request.getString("database")).getCollection(request.getString("collection"))
                    .get(request.getString("identifier")).ifPresentOrElse(roseEntity -> RoseServer.reply(context, roseEntity.get(), unique, 1),
                    () -> RoseServer.reply(context, "No results.", unique,0));
        }
    }
}
