package pw.mihou.rosedb.io;

import org.json.JSONObject;
import pw.mihou.rosedb.RoseDB;
import pw.mihou.rosedb.io.entities.QueryRequest;

public class RoseQuery {

    /**
     * Parses the message into Query Request.
     *
     * @param message The message to parse.
     * @return The Query request parsed (usually every value is null inside if invalid).
     */
    public static QueryRequest parse(String message){
        try {
            String[] context = message.split("\\.");
            QueryRequest request = new QueryRequest();
            if (context.length > 2) {
                request.database = context[0];
                request.collection = context[1];
                request.method = context[2].substring(0, context[2].indexOf("("));
                if (!context[2].endsWith("()")) {
                    String item = context[2].replaceFirst(request.method + "\\(", "");
                    item = item.substring(0, !item.contains(", ") ? item.indexOf(")") : item.indexOf(", "));
                    request.identifier = item;
                    if(context[2].contains(", ")) {
                        String req = context[2].replaceFirst(request.method + "\\(" + item + ", ", "");
                        req = req.substring(0, req.length() - 1);
                        request.value = req;
                    }
                }
            } else {
                request.database = context[0];
                request.method = context[1].substring(0, context[1].indexOf("("));
                if (!context[1].endsWith("()")) {
                    String item = context[1].replaceFirst(request.method + "\\(", "");
                    item = item.substring(0, !item.contains(", ") ? item.indexOf(")") : item.indexOf(", "));
                    request.identifier = item;
                    if(context[1].contains(", ")) {
                        String req = context[1].replaceFirst(request.method + "\\(" + item + ", ", "");
                        req = req.substring(0, req.length() - 1);
                        request.value = req;
                    }
                }
            }
            return request;
        } catch (IndexOutOfBoundsException e){
            return new QueryRequest();
        }
    }

    /**
     * Parses a JSONObject into Queria's Query Request.
     *
     * @param object The JSON Object to parse.
     * @return The Query request parsed (usually every value is null inside if invalid).
     */
    public static QueryRequest parse(JSONObject object){
        return RoseDB.gson.fromJson(object.toString(), QueryRequest.class);
    }

}
