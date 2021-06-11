package pw.mihou.rosedb.manager;

import io.javalin.websocket.WsContext;
import org.json.JSONObject;
import pw.mihou.rosedb.io.Scheduler;
import pw.mihou.rosedb.manager.entities.RoseListener;

import java.util.ArrayList;
import java.util.List;

public class RoseListenerManager {

    private static final List<RoseListener> listeners = new ArrayList<>();

    public static void register(RoseListener listener) {
        listeners.add(listener);
    }

    public static void execute(JSONObject request, WsContext context) {
        listeners.stream().filter(roseListener -> roseListener.type().value.equalsIgnoreCase(request.getString("method")))
                .forEachOrdered(roseListener -> Scheduler.submit(() -> roseListener.execute(request, context, request.getString("unique"))));
    }

}
