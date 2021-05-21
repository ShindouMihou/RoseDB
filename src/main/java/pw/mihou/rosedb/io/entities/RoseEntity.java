package pw.mihou.rosedb.io.entities;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Optional;

public class RoseEntity {

    private final JSONObject data;

    public RoseEntity(HashMap<String, String> data){
        this.data = new JSONObject();
        data.forEach(data::put);
    }

    public RoseEntity(String key, String value){
        this.data = new JSONObject();
        this.data.put(key, value);
    }

    public RoseEntity(String json){
        this.data = new JSONObject(json);
    }

    public RoseEntity add(String key, String value){
        this.data.put(key, value);
        return this;
    }

    public Optional<String> get(String key){
        return Optional.ofNullable(data.getString(key));
    }

    public RoseEntity remove(String key){
        this.data.remove(key);
        return this;
    }

    public String get(){
        return data.toString();
    }

}
