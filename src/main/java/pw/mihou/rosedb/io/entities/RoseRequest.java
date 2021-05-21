package pw.mihou.rosedb.io.entities;

import java.util.HashMap;
import java.util.Map;

public class RoseRequest {

    public String json;
    public String database;
    public String collection;
    public String identifier;
    private Map<String, String> request;

    public RoseRequest(String database, String collection, String identifier, Map<String, String> request) {
        this.request = request;
        this.database = database;
        this.collection = collection;
        this.identifier = identifier;
    }

    public RoseRequest(String database, String collection, String identifier, String request) {
        this.json = request;
        this.database = database;
        this.collection = collection;
        this.identifier = identifier;
    }

    public RoseRequest(String database, String collection, String identifier, String key, String value) {
        this.request = new HashMap<>();
        request.put(key, value);
        this.database = database;
        this.collection = collection;
        this.identifier = identifier;
    }

    public Map<String, String> getRequest() {
        return this.request;
    }

}
