package pw.mihou.rosedb.listeners;

import io.javalin.websocket.WsContext;
import org.json.JSONObject;
import pw.mihou.rosedb.connections.RoseServer;
import pw.mihou.rosedb.enums.Listening;
import pw.mihou.rosedb.manager.RoseCollections;
import pw.mihou.rosedb.manager.entities.RoseListener;

public class AddListener implements RoseListener {
    @Override
    public Listening type() {
        return Listening.ADD;
    }

    @Override
    public void execute(JSONObject request, WsContext context) {
        if (request.isNull("value") || request.isNull("identifier")
                || request.isNull("database") || request.isNull("collection")) {
            RoseServer.reply(context, "Missing parameters either: [value], [identifier], [database], [collection]", -1);
        } else {
            RoseCollections collections = RoseServer.getDatabase(request.getString("database")).getCollection(request.getString("collection"));
            collections.add(request.getString("identifier"), request.getString("value"));
            collections.get(request.getString("identifier")).ifPresentOrElse(roseEntity -> RoseServer.reply(context, roseEntity.get(), 1),
                    () -> RoseServer.reply(context, "Failed to update value.", 0));
        }
    }
}
