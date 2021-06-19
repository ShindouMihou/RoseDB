package pw.mihou.rosedb.manager.entities;

import org.java_websocket.WebSocket;
import org.json.JSONObject;
import pw.mihou.rosedb.enums.Listening;
import pw.mihou.rosedb.io.entities.QueryRequest;

public interface RoseListener {

    /**
     * The type of listener.
     * This will be used to check whether the listener is able
     * to take in a certain request.
     *
     * @return The listener type.
     */
    Listening type();

    /**
     * Receives an request and handles it appropriately.
     *
     * @param request The request that was sent from the client.
     * @param context The websocket context of the client.
     * @param unique The unique callback of the client.
     */
    void execute(QueryRequest request, WebSocket context, String unique);

    /**
     * Receives an request and handles it appropriately.
     *
     * @param request The request that was sent from the client.
     * @param context The websocket context of the client.
     * @param unique The unique callback of the client.
     */
    void execute(JSONObject request, WebSocket context, String unique);
}
