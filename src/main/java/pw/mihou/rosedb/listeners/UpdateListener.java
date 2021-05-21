package pw.mihou.rosedb.listeners;

import io.javalin.websocket.WsContext;
import org.json.JSONObject;
import pw.mihou.rosedb.connections.RoseServer;
import pw.mihou.rosedb.enums.Listening;
import pw.mihou.rosedb.manager.RoseCollections;
import pw.mihou.rosedb.manager.entities.RoseListener;

public class UpdateListener implements RoseListener {
    @Override
    public Listening type() {
        return Listening.UPDATE;
    }

    @Override
    public void execute(JSONObject request, WsContext context, String unique) {
        if (request.isNull("value") || request.isNull("identifier") || request.isNull("database") || request.isNull("collection")) {
            RoseServer.reply(context, "Missing parameters either: [value], [identifier], [database], [collection]", unique,-1);
        } else {
            RoseCollections collections = RoseServer.getDatabase(request.getString("database")).getCollection(request.getString("collection"));
            collections.add(request.getString("identifier"), request.getString("value"));
            collections.get(request.getString("identifier")).ifPresentOrElse(roseEntity -> RoseServer.reply(context, roseEntity.get(), unique, 1),
                    () -> RoseServer.reply(context, "Failed to update value.", unique, 0));
        }
    }
}
