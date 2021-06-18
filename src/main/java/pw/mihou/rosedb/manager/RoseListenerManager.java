package pw.mihou.rosedb.manager;

import io.javalin.websocket.WsContext;
import org.json.JSONObject;
import pw.mihou.rosedb.connections.RoseServer;
import pw.mihou.rosedb.io.Scheduler;
import pw.mihou.rosedb.io.entities.QueryRequest;
import pw.mihou.rosedb.manager.entities.RoseListener;

import java.util.ArrayList;
import java.util.List;

public class RoseListenerManager {

    private static final List<RoseListener> listeners = new ArrayList<>();

    public static void register(RoseListener listener) {
        listeners.add(listener);
    }

    public static void execute(QueryRequest request, WsContext context) {
        if(request.database != null && request.database.equalsIgnoreCase(".rose_secrets")){
            RoseServer.reply(context, "Requested directory is protected by RoseDB.", request.unique, -1);
            return;
        }

        listeners.stream().filter(roseListener -> roseListener.type().value.equalsIgnoreCase(request.method))
                .forEachOrdered(roseListener -> Scheduler.getExecutorService().submit(() -> roseListener.execute(request, context,
                        request.valueAsJSONObject().isNull("unique") ? request.unique : request.valueAsJSONObject().getString("unique"))));
    }

    public static void execute(JSONObject request, WsContext context) {
        if(!request.isNull("database") && request.getString("database").equalsIgnoreCase(".rose_secrets")){
            RoseServer.reply(context, "Requested directory is protected by RoseDB.", request.getString("unique"), -1);
            return;
        }

        listeners.stream().filter(roseListener -> roseListener.type().value.equalsIgnoreCase(request.getString("method")))
                .forEachOrdered(roseListener -> Scheduler.submit(() -> roseListener.execute(request, context, request.getString("unique"))));
    }

}
