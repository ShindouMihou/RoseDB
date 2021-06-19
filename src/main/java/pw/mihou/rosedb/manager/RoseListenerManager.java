package pw.mihou.rosedb.manager;

import org.java_websocket.WebSocket;
import org.json.JSONObject;
import pw.mihou.rosedb.connections.RoseServer;
import pw.mihou.rosedb.enums.Levels;
import pw.mihou.rosedb.io.Scheduler;
import pw.mihou.rosedb.io.entities.QueryRequest;
import pw.mihou.rosedb.listeners.*;
import pw.mihou.rosedb.manager.entities.RoseListener;
import pw.mihou.rosedb.utility.Terminal;

import java.util.ArrayList;
import java.util.List;

public class RoseListenerManager {

    /**
     * The list of listeners that will be listening
     * to all requests.
     */
    private static final List<RoseListener> listeners = new ArrayList<>();

    /*
      A method that will register all listeners
      ahead of time.

      If you are adding a new listener, please add
      it below the last one.
     */
    static {
        listeners.add(new RequestListener());
        listeners.add(new AddListener());
        listeners.add(new DeleteListener());
        listeners.add(new UpdateListener());
        listeners.add(new DropListener());
        listeners.add(new AggregateListener());
        listeners.add(new RevertListener());
    }

    /**
     * Checks the value of the field "method" and
     * looks through the list of listeners to see if any listener
     * is able to accept the method requested and fires the event for that listener.
     *
     * @param request The request sent from the client.
     * @param context The websocket context of the client.
     */
    public static void execute(QueryRequest request, WebSocket context) {
        if(request.database != null && request.database.equalsIgnoreCase(".rose_secrets")){
            RoseServer.reply(context, "Requested directory is protected by RoseDB.", request.unique, -1);
            return;
        }

        listeners.stream().filter(roseListener -> roseListener.type().value.equalsIgnoreCase(request.method))
                .forEachOrdered(roseListener -> Scheduler.getExecutorService().submit(() -> roseListener.execute(request, context,
                        request.value != null ? (request.valueAsJSONObject().isNull("unique")
                                ? request.unique : request.valueAsJSONObject().getString("unique")) : request.unique)));
    }


    /**
     * Checks the value of the field "method" and
     * looks through the list of listeners to see if any listener
     * is able to accept the method requested and fires the event for that listener.
     *
     * @param request The request sent from the client.
     * @param context The websocket context of the client.
     */
    public static void execute(JSONObject request, WebSocket context) {
        if(!request.isNull("database") && request.getString("database").equalsIgnoreCase(".rose_secrets")){
            RoseServer.reply(context, "Requested directory is protected by RoseDB.", request.getString("unique"), -1);
            return;
        }

        listeners.stream().filter(roseListener -> roseListener.type().value.equalsIgnoreCase(request.getString("method")))
                .forEachOrdered(roseListener -> Scheduler.submit(() -> roseListener.execute(request, context, request.getString("unique"))));
    }

}
