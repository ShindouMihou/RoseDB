package pw.mihou.rosedb.manager;

import io.javalin.websocket.WsContext;
import org.json.JSONObject;
import pw.mihou.rosedb.RoseDB;
import pw.mihou.rosedb.connections.RoseServer;
import pw.mihou.rosedb.io.Scheduler;
import pw.mihou.rosedb.manager.entities.RoseListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RoseListenerManager {

    private static final List<RoseListener> listeners = new ArrayList<>();

    public static void register(RoseListener listener) {
        listeners.add(listener);
    }

    public static void execute(JSONObject request, WsContext context) {
        if (!request.isNull("unique") && ((!(request.isNull("authorization"))
                && request.getString("authorization").equals(RoseDB.authorization))
                || Optional.ofNullable(context.header("authorization")).orElse("none").equals(RoseDB.authorization))) {
            listeners.stream().filter(roseListener -> roseListener.type().value.equalsIgnoreCase(request.getString("method")))
                    .forEach(roseListener -> Scheduler.submit(() -> roseListener.execute(request, context, request.getString("unique"))));
        } else {
            RoseServer.reply(context, "Please validate: correct authorization code or unique value on request.", -1);
        }
    }

}
