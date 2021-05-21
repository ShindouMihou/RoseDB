package pw.mihou.rosedb.listeners;

import io.javalin.websocket.WsContext;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import pw.mihou.rosedb.connections.RoseServer;
import pw.mihou.rosedb.enums.Listening;
import pw.mihou.rosedb.manager.RoseCollections;
import pw.mihou.rosedb.manager.entities.RoseListener;

public class DeleteListener implements RoseListener {
    @Override
    public Listening type() {
        return Listening.DELETE;
    }

    @Override
    public void execute(JSONObject request, WsContext context, String unique) {
        if (request.isNull("identifier") || request.isNull("database") || request.isNull("collection")) {
            RoseServer.reply(context, "Missing parameters either: [key], [identifier], [database], [collection]", unique,-1);
        } else {
            if(request.isNull("key")) {
                RoseServer.getDatabase(request.getString("database")).getCollection(request.getString("collection"))
                        .delete(request.getString("identifier"));
                RoseServer.reply(context, "The item: " + request.getString("identifier") + " was deleted.", unique, 1);
            } else if(!request.isNull("key")){
                RoseCollections collections = RoseServer.getDatabase(request.getString("database")).getCollection(request.getString("collection"));
                collections.get(request.getString("identifier")).ifPresentOrElse(roseEntity -> {
                    try {
                        JSONObject object = new JSONObject(roseEntity.get());
                        if(request.get("key") instanceof JSONArray){
                            JSONArray key = request.getJSONArray("key");

                            for(int i = 0; i < key.length(); i++){
                                object.remove(key.getString(i));
                            }
                        } else {
                            object.remove(request.getString("key"));
                        }

                        collections.add(request.getString("identifier"), object.toString());
                        RoseServer.reply(context, object.toString(), unique, 1);
                    } catch (JSONException e){
                        RoseServer.reply(context, request.getString("identifier") + " reported as invalid JSON, did you perhaps change it manually: " + e.getMessage(), unique, -1);
                    }
                }, () -> RoseServer.reply(context, "No results for identifier: " + request.getString("identifier"), unique, -1));
            } else {
                RoseServer.reply(context, "Missing parameters either: [key], [identifier], [database], [collection]", unique,-1);
            }
        }
    }
}
