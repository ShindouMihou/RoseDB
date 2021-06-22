package pw.mihou.rosedb.io.entities;

import org.json.JSONObject;
import pw.mihou.rosedb.RoseDB;

public class QueryRequest {

    /**
     * The database from the request.
     * This should never be null if the syntax is really of Queria.
     */
    public String database;

    /**
     * The collection from the request.
     * This is usually null in cases like database.aggregate()
     * or database.drop().
     */
    public String collection;

    /**
     * The identifier of the item from the request.
     * This is usually null especially in cases like .aggregate().
     */
    public String identifier;

    /**
     * The method to use from the request.
     * This should never be null unless the request is not of Queria.
     */
    public String method;

    /**
     * The value from the request.
     * This is sometimes null especially in cases like .get(item) or
     * .aggregate() or drop(item)
     */
    public String value;

    /**
     * The unique callback from the request.
     * This is usually empty if it comes from terminal.
     */
    public String unique = "";

    /**
     * Checks if the query result is valid.
     * This involves checking if database value and method value is not null.
     *
     * @return Whether the Query Result is of valid Queria syntax.
     */
    public boolean isValid(){
        return database != null && method != null;
    }

    /**
     * Parses the request into JSONObject.
     *
     * @return A JSONObject variant of the request.
     */
    public JSONObject asJSONObject(){
        return new JSONObject(RoseDB.gson.toJson(this));
    }

    /**
     * Converts only the value into JSON Object.
     *
     * @return A JSONObject containing only the value of the request.
     */
    public JSONObject valueAsJSONObject(){
        return new JSONObject(value);
    }

}
