package pw.mihou.rosedb.manager;

import pw.mihou.rosedb.io.FileHandler;
import pw.mihou.rosedb.io.entities.RoseEntity;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class RoseCollections {

    private final Map<String, RoseEntity> data = new ConcurrentHashMap<>();
    private final String collection;
    private final String database;

    public RoseCollections(String collection, String database) {
        this.collection = collection;
        this.database = database;
    }

    public void cache(String identifier, String json) {
        this.data.put(identifier, new RoseEntity(json));
    }

    public void delete(String identifier) {
        this.data.remove(identifier);
        CompletableFuture.runAsync(() -> FileHandler.delete(database, collection, identifier));
    }

    public void add(String identifier, String json) {
        this.data.put(identifier, new RoseEntity(json));
        CompletableFuture.runAsync(() -> FileHandler.write(database, collection, identifier, data.get(identifier).get()));
    }

    public Optional<RoseEntity> get(String identifier) {
        if (!data.containsKey(identifier))
            FileHandler.readData(database, collection, identifier).ifPresent(roseEntity -> data.put(identifier, roseEntity));

        return Optional.ofNullable(data.get(identifier));
    }

}
