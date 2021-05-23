package pw.mihou.rosedb.manager;

import pw.mihou.rosedb.io.FileHandler;
import pw.mihou.rosedb.io.Scheduler;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class RoseCollections {

    private final Map<String, String> data = new ConcurrentHashMap<>();
    public final String collection;
    private final String database;

    public RoseCollections(String collection, String database) {
        this.collection = collection;
        this.database = database;
    }

    public void cache(String identifier, String json) {
        this.data.put(identifier, json);
    }

    public void delete(String identifier) {
        this.data.remove(identifier);
        Scheduler.getExecutorService().submit(() -> FileHandler.delete(database, collection, identifier));
    }

    public Map<String, String> getData(){
        return data;
    }

    public void add(String identifier, String json) {
        this.data.put(identifier, json);
        FileHandler.write(database, collection, identifier, json);
    }

    public Optional<String> get(String identifier) {
        if (!data.containsKey(identifier)) {
            FileHandler.readData(database, collection, identifier).ifPresent(roseEntity -> data.put(identifier, roseEntity));
        }

        return Optional.ofNullable(data.get(identifier));
    }

}
