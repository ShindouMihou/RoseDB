package pw.mihou.rosedb.io.entities;

import java.util.HashMap;
import java.util.Map;

public class RoseRequest {

    public String json;
    public String database;
    public String collection;
    public String identifier;
    private Map<String, String> request;

    /**
     * Creates a Rose Request with a key-value request.
     * @param database The database where the item is held.
     * @param collection The collection where the item is held.
     * @param identifier The identifier of the item.
     * @param request The key-value request.
     */
    public RoseRequest(String database, String collection, String identifier, Map<String, String> request) {
        this.request = request;
        this.database = database;
        this.collection = collection;
        this.identifier = identifier;
    }

    /**
     * Creates a Rose Request with a String request which is usually a JSON value.
     * @param database The database where the item is held.
     * @param collection The collection where the item is held.
     * @param identifier The identifier of the item.
     * @param request The JSON value inside the item.
     */
    public RoseRequest(String database, String collection, String identifier, String request) {
        this.json = request;
        this.database = database;
        this.collection = collection;
        this.identifier = identifier;
    }

    /**
     * Creates a key-value Rose Request.
     * @param database The database where the item is held.
     * @param collection The collection where the item is held.
     * @param identifier The identifier of the itme.
     * @param key The key of the value.
     * @param value The value of the key.
     */
    public RoseRequest(String database, String collection, String identifier, String key, String value) {
        this.request = new HashMap<>();
        request.put(key, value);
        this.database = database;
        this.collection = collection;
        this.identifier = identifier;
    }

    /**
     * Retrieves the key-value request.
     *
     * @return The key-value map.
     */
    public Map<String, String> getRequest() {
        return this.request;
    }

    /**
     * Compares the two requests to figure out whether they are
     * heading to the same item or file.
     *
     * @param request The request to compare.
     * @return Are they heading to the same file?
     */
    public boolean compare(RoseRequest request){
        return identifier.equals(request.identifier) && database.equals(request.database) &&
                collection.equals(request.collection);
    }

}
