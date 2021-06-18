package pw.mihou.rosedb.listeners;

import io.javalin.websocket.WsContext;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import pw.mihou.rosedb.connections.RoseServer;
import pw.mihou.rosedb.enums.Listening;
import pw.mihou.rosedb.io.RoseQuery;
import pw.mihou.rosedb.io.entities.QueryRequest;
import pw.mihou.rosedb.manager.RoseCollections;
import pw.mihou.rosedb.manager.entities.RoseListener;

public class DeleteListener implements RoseListener {
    @Override
    public Listening type() {
        return Listening.DELETE;
    }

    @Override
    public void execute(QueryRequest request, WsContext context, String unique) {
        JSONObject val = request.valueAsJSONObject();
        handle(request, val.isNull("key") ? null : val.get("val"), context, unique);
    }

    @Override
    public void execute(JSONObject request, WsContext context, String unique) {
        handle(RoseQuery.parse(request), request.isNull("key") ? null : request.get("key"), context, unique);
    }

    private void handle(QueryRequest request, @Nullable Object keys, WsContext context, String unique){
        if(request.identifier == null || request.database == null || request.collection == null){
            RoseServer.reply(context, "Missing parameters either: [key], [identifier], [database], [collection]", unique, -1);
        } else {
            if(keys == null){
                RoseServer.getDatabase(request.database).getCollection(request.collection).delete(request.identifier);
                RoseServer.reply(context, "The item: " + request.identifier + " was deleted.", unique, 1);
            } else {
                RoseCollections collections = RoseServer.getDatabase(request.database).getCollection(request.collection);
                collections.get(request.identifier).ifPresentOrElse(s -> {
                    try {
                        JSONObject object = new JSONObject(s);
                        if(keys instanceof JSONArray){
                            JSONArray key = (JSONArray) keys;

                            for(int i = 0; i < key.length(); i++){
                                object.remove(key.getString(i));
                            }
                        } else if(keys instanceof String){
                            object.remove((String) keys);
                        }
                    } catch (JSONException e) {
                        RoseServer.reply(context, request.identifier + " was reported as invalid JSON, did you perhaps change it manually: " + e.getMessage(), unique, -1);
                    }
                }, () -> RoseServer.reply(context, "No results for identifier: " + request.identifier, unique, 0));
            }
        }
    }
}
