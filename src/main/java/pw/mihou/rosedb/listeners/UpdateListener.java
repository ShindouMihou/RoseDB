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

public class UpdateListener implements RoseListener {
    @Override
    public Listening type() {
        return Listening.UPDATE;
    }

    @Override
    public void execute(QueryRequest request, WsContext context, String unique) {
        JSONObject val = request.valueAsJSONObject();
        handle(request, val.isNull("key") ? null : val.get("key"), val.isNull("value") ? null : val.get("value"),
                context, unique);
    }

    @Override
    public void execute(JSONObject request, WsContext context, String unique) {
        handle(RoseQuery.parse(request), request.isNull("key") ? null : request.get("key"), request.isNull("value") ? null : request.get("value"),
                context, unique);
    }

    private void handle(QueryRequest request, @Nullable Object key, @Nullable Object value, WsContext context, String unique){
        if(request.identifier == null || request.database == null || request.collection == null){
            RoseServer.reply(context, "Missing parameters either: [value], [key], [identifier], [database], [collection]", unique, -1);
        } else {
            if(key == null && value != null){
                RoseServer.reply(context, RoseServer.getDatabase(request.database)
                .getCollection(request.collection).add(request.identifier, request.value), unique, 1);
            } else if(key != null && value != null){
                RoseCollections collections = RoseServer.getDatabase(request.database).getCollection(request.collection);
                collections.get(request.identifier).ifPresentOrElse(s -> {
                    try {
                        JSONObject object = new JSONObject(s);
                        if(key instanceof JSONArray && value instanceof JSONArray){
                            JSONArray keys = (JSONArray) key;
                            JSONArray values = (JSONArray) value;

                            for(int i = 0; i < keys.length(); i++){
                                object.put(keys.getString(i), values.get(i));
                            }
                        } else if(key instanceof String){
                            object.put((String) key, value);
                        }

                        collections.add(request.identifier, object.toString());
                        RoseServer.reply(context, object.toString(), unique, 1);
                    } catch (JSONException e){
                        RoseServer.reply(context, request.identifier + " reported as invalid JSON, did you perhaps change it manually: " + e.getMessage(),
                                unique, -1);
                    }
                }, () -> RoseServer.reply(context, "No results for identifier: " + request.identifier, unique, 0));
            } else {
                RoseServer.reply(context, "Missing parameters: [key], [value] or [value]", unique, -1);
            }
        }
    }
}
