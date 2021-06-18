package pw.mihou.rosedb.manager.entities;

import io.javalin.websocket.WsContext;
import org.json.JSONObject;
import pw.mihou.rosedb.enums.Listening;
import pw.mihou.rosedb.io.entities.QueryRequest;

public interface RoseListener {

    Listening type();

    void execute(QueryRequest request, WsContext context, String unique);
    void execute(JSONObject request, WsContext context, String unique);
}
