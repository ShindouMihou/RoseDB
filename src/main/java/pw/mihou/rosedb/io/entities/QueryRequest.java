package pw.mihou.rosedb.io.entities;

import org.json.JSONObject;
import pw.mihou.rosedb.RoseDB;

public class QueryRequest {

    public String database;
    public String collection;
    public String identifier;
    public String method;
    public String value;
    public String unique = "";

    public boolean isValid(){
        return database != null && method != null;
    }

    public boolean isComplete(){
        return database != null && collection != null && identifier != null && method != null && value != null;
    }

    public JSONObject asJSONObject(){
        return new JSONObject(RoseDB.gson.toJson(this));
    }

    public JSONObject valueAsJSONObject(){
        return new JSONObject(value);
    }

}
