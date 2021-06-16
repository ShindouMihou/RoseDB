package pw.mihou.rosedb.listeners;

import io.javalin.websocket.WsContext;
import org.json.JSONObject;
import pw.mihou.rosedb.connections.RoseServer;
import pw.mihou.rosedb.enums.Levels;
import pw.mihou.rosedb.enums.Listening;
import pw.mihou.rosedb.manager.RoseCollections;
import pw.mihou.rosedb.manager.entities.RoseListener;
import pw.mihou.rosedb.utility.Terminal;

public class AddListener implements RoseListener {
    @Override
    public Listening type() {
        return Listening.ADD;
    }

    @Override
    public void execute(JSONObject request, WsContext context, String unique) {
        if (request.isNull("value") || request.isNull("identifier")
                || request.isNull("database") || request.isNull("collection")) {
            RoseServer.reply(context, "Missing parameters either: [value], [identifier], [database], [collection]", unique, -1);
        } else {
            Terminal.log(Levels.DEBUG, "Request to add {} with value {}", request.getString("identifier"), request.getString("value"));
            RoseCollections collections = RoseServer.getDatabase(request.getString("database")).getCollection(request.getString("collection"));
            RoseServer.reply(context, collections.add(request.getString("identifier"), request.getString("value")), unique, 1);
        }
    }
}
