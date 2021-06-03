package pw.mihou.rosedb.manager;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import pw.mihou.rosedb.io.FileHandler;
import pw.mihou.rosedb.io.Scheduler;

import java.util.Map;
import java.util.Optional;

public class RoseCollections {

    private final LoadingCache<String, String> data;
    public final String collection;
    private final String database;

    public RoseCollections(String collection, String database) {
        this.collection = collection;
        this.database = database;
        // We are returning null since Caffeine will not add null values.
        this.data = Caffeine.newBuilder()
                .build(key -> FileHandler.readData(database, collection, key)
                        .orElse(null));
    }

    public void cache(String identifier, String json) {
        this.data.put(identifier, json);
    }

    public void delete(String identifier) {
        this.data.invalidate(identifier);
        Scheduler.submit(() -> FileHandler.delete(database, collection, identifier));
    }

    public Map<String, String> getData(){
        return data.asMap();
    }

    public void add(String identifier, String json) {
        this.data.put(identifier, json);
        FileHandler.write(database, collection, identifier, json);
    }

    public Optional<String> get(String identifier) {
        return Optional.ofNullable(data.get(identifier));
    }

}
