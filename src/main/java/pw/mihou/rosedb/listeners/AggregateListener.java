package pw.mihou.rosedb.listeners;

import io.javalin.websocket.WsContext;
import org.json.JSONObject;
import pw.mihou.rosedb.connections.RoseServer;
import pw.mihou.rosedb.enums.Listening;
import pw.mihou.rosedb.manager.RoseCollections;
import pw.mihou.rosedb.manager.entities.RoseListener;

import java.util.stream.Collectors;

public class AggregateListener implements RoseListener {
    @Override
    public Listening type() {
        return Listening.AGGREGATE;
    }

    @Override
    public void execute(JSONObject request, WsContext context, String unique) {
        if(request.isNull("collection") && !request.isNull("database")){
            RoseServer.reply(context, new JSONObject().put(request.getString("database"), RoseServer.getDatabase(request.getString("database")).getCollections()
                    .stream().collect(Collectors.toMap(collections -> collections.collection, RoseCollections::getData))),
                    unique, 1);
        } else if(!request.isNull("collection") && !request.isNull("database")){
            RoseServer.reply(context, new JSONObject().put(request.getString("collection"),
                    RoseServer.getDatabase(request.getString("database")).getCollection(request.getString("collection")).getData()),
                    unique, 1);
        }
    }
}
