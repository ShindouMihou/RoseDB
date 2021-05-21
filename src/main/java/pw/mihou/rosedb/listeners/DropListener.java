package pw.mihou.rosedb.listeners;

import io.javalin.websocket.WsContext;
import org.json.JSONObject;
import pw.mihou.rosedb.connections.RoseServer;
import pw.mihou.rosedb.enums.Listening;
import pw.mihou.rosedb.manager.entities.RoseListener;

import java.io.IOException;

public class DropListener implements RoseListener {
    @Override
    public Listening type() {
        return Listening.DROP;
    }

    @Override
    public void execute(JSONObject request, WsContext context, String unique) {
        if (request.isNull("database") && request.isNull("collection") || request.isNull("database") && !request.isNull("collection")) {
            RoseServer.reply(context, "Missing parameters either: [database], [collection]", unique, -1);
        } else {
            try {
                if (request.isNull("collection")) {
                    RoseServer.removeDatabase(request.getString("database"));
                    RoseServer.reply(context, "Successfully deleted the database " + request.getString("database"), unique, 1);
                } else {
                    RoseServer.getDatabase(request.getString("database")).removeCollection(request.getString("collection"));
                    RoseServer.reply(context, "Successfully deleted the collection " + request.getString("collection"), unique,1);
                }
            } catch (IOException exception) {
                RoseServer.reply(context, "An exception occurred: " + exception.getMessage(), unique,-1);
            }
        }
    }
}
