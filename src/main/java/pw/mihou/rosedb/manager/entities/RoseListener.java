package pw.mihou.rosedb.manager.entities;

import io.javalin.websocket.WsContext;
import org.json.JSONObject;
import pw.mihou.rosedb.enums.Listening;

public interface RoseListener {

    Listening type();

    void execute(JSONObject request, WsContext context, String unique);

}
