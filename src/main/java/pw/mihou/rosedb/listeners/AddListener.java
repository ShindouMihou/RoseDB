package pw.mihou.rosedb.listeners;

import org.java_websocket.WebSocket;
import org.json.JSONException;
import org.json.JSONObject;
import pw.mihou.rosedb.connections.RoseServer;
import pw.mihou.rosedb.enums.Levels;
import pw.mihou.rosedb.enums.Listening;
import pw.mihou.rosedb.io.entities.QueryRequest;
import pw.mihou.rosedb.manager.RoseCollections;
import pw.mihou.rosedb.manager.RoseDatabase;
import pw.mihou.rosedb.manager.entities.RoseListener;
import pw.mihou.rosedb.utility.Pair;
import pw.mihou.rosedb.utility.Terminal;

public class AddListener implements RoseListener {
    @Override
    public Listening type() {
        return Listening.ADD;
    }

    @Override
    public void execute(QueryRequest request, WebSocket context, String unique) {
        try {
            execute(request.asJSONObject(), context, unique);
        } catch (JSONException e){
            RoseServer.reply(context, "The request was considered as invalid: " + e.getMessage(), unique, -1);
        }
    }

    @Override
    public void execute(JSONObject request, WebSocket context, String unique) {
        if (request.isNull("value") || request.isNull("identifier")
                || request.isNull("database") || request.isNull("collection")) {
            RoseServer.reply(context, "Missing parameters either: [value], [identifier], [database], [collection]", unique, -1);
        } else {
            Terminal.log(Levels.DEBUG, "Request to add {} with value {}", request.getString("identifier"), request.getString("value"));
            RoseCollections collections = RoseDatabase.getDatabase(request.getString("database")).getCollection(request.getString("collection"));
            Pair<Integer, String> response = collections.add(request.getString("identifier"), request.getString("value"));
            RoseServer.reply(context, response.getRight(), unique, response.getLeft());
        }
    }
}
