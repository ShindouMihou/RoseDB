package pw.mihou.rosedb.manager;

import io.javalin.websocket.WsContext;
import org.json.JSONObject;
import pw.mihou.rosedb.RoseDB;
import pw.mihou.rosedb.connections.RoseServer;
import pw.mihou.rosedb.manager.entities.RoseListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class RoseListenerManager {

    private static final List<RoseListener> listeners = new ArrayList<>();

    public static void register(RoseListener listener) {
        listeners.add(listener);
    }

    public static void remove(RoseListener listener) {
        listeners.remove(listener);
    }

    public static void execute(JSONObject request, WsContext context) {
        if (!(request.isNull("authorization")) && request.getString("authorization").equalsIgnoreCase(RoseDB.authorization)) {
            listeners.stream().filter(roseListener -> roseListener.type().value.equalsIgnoreCase(request.getString("method")))
                    .forEach(roseListener -> CompletableFuture.runAsync(() -> roseListener.execute(request, context)));
        } else {
            RoseServer.reply(context, "Invalid authorization code.", -1);
        }
    }

}
