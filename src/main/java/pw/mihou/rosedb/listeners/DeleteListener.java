package pw.mihou.rosedb.listeners;

import io.javalin.websocket.WsContext;
import org.json.JSONObject;
import pw.mihou.rosedb.connections.RoseServer;
import pw.mihou.rosedb.enums.Listening;
import pw.mihou.rosedb.manager.entities.RoseListener;

public class DeleteListener implements RoseListener {
    @Override
    public Listening type() {
        return Listening.DELETE;
    }

    @Override
    public void execute(JSONObject request, WsContext context) {
        if(request.isNull("identifier") || request.isNull("database") || request.isNull("collection")){
            RoseServer.reply(context, "Missing parameters either: [identifier], [database], [collection]", -1);
        } else {
            RoseServer.getDatabase(request.getString("database")).getCollection(request.getString("collection"))
                    .delete(request.getString("identifier"));
            RoseServer.reply(context, "Successful", 1);
        }
    }
}
