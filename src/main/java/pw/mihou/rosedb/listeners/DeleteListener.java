package pw.mihou.rosedb.listeners;

import org.java_websocket.WebSocket;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import pw.mihou.rosedb.connections.RoseServer;
import pw.mihou.rosedb.enums.Levels;
import pw.mihou.rosedb.enums.Listening;
import pw.mihou.rosedb.io.RoseQuery;
import pw.mihou.rosedb.io.entities.QueryRequest;
import pw.mihou.rosedb.manager.RoseCollections;
import pw.mihou.rosedb.manager.RoseDatabase;
import pw.mihou.rosedb.manager.entities.RoseListener;
import pw.mihou.rosedb.utility.Terminal;

public class DeleteListener implements RoseListener {
    @Override
    public Listening type() {
        return Listening.DELETE;
    }

    @Override
    public void execute(QueryRequest request, WebSocket context, String unique) {
        Terminal.log(Levels.DEBUG, "Request: {} with value: {}?", request.asJSONObject().toString(), request.value != null);
        if(request.value != null) {
            JSONObject val = request.valueAsJSONObject();
            handle(request, val.isNull("key") ? null : val.get("key"), context, unique);
        } else {
            handle(request, null, context, unique);
        }
    }

    @Override
    public void execute(JSONObject request, WebSocket context, String unique) {
        handle(RoseQuery.parse(request), request.isNull("key") ? null : request.get("key"), context, unique);
    }

    private void handle(QueryRequest request, @Nullable Object keys, WebSocket context, String unique){
        if(request.identifier == null || request.database == null || request.collection == null){
            RoseServer.reply(context, "Missing parameters either: [key], [identifier], [database], [collection]", unique, -1);
        } else {
            if(keys == null){
                RoseDatabase.getDatabase(request.database).getCollection(request.collection).delete(request.identifier);
                RoseServer.reply(context, "The item: " + request.identifier + " was deleted.", unique, 1);
            } else {
                RoseCollections collections = RoseDatabase.getDatabase(request.database).getCollection(request.collection);
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

                        collections.add(request.identifier, object.toString());
                        RoseServer.reply(context, object.toString(), unique, 1);
                    } catch (JSONException e) {
                        RoseServer.reply(context, request.identifier + " was reported as invalid JSON, did you perhaps change it manually: " + e.getMessage(), unique, -1);
                    }
                }, () -> RoseServer.reply(context, "No results for identifier: " + request.identifier, unique, 0));
            }
        }
    }
}
