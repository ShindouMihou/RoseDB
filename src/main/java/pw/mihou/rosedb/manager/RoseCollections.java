package pw.mihou.rosedb.manager;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import pw.mihou.rosedb.RoseDB;
import pw.mihou.rosedb.enums.Levels;
import pw.mihou.rosedb.io.FileHandler;
import pw.mihou.rosedb.io.Scheduler;
import pw.mihou.rosedb.utility.Pair;
import pw.mihou.rosedb.utility.Terminal;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class RoseCollections {

    private final LoadingCache<String, String> data;
    private LoadingCache<String, String> versions;
    public final String collection;
    private final String database;

    public RoseCollections(String collection, String database) {
        this.collection = collection;
        this.database = database;
        // We are returning null since Caffeine will not add null values.
        this.data = Caffeine.newBuilder()
                .build(key -> FileHandler.readData(database, collection, key)
                        .orElse(null));

        if(RoseDB.versioning) {
            versions = Caffeine.newBuilder().expireAfterWrite(6, TimeUnit.HOURS).build(key ->
                    FileHandler.readVersion(database, collection, key).orElse(null));
        }
    }

    public void cache(String identifier, String json) {
        this.data.put(identifier, json);
    }

    public Pair<Boolean, String> revert(String identifier){
        if(versions.get(identifier) == null)
            return new Pair<>(false, "");

        String json = versions.get(identifier);
        versions.invalidate(identifier);

        add(identifier, json);
        return new Pair<>(true, json);
    }

    public void delete(String identifier) {
        this.data.invalidate(identifier);
        Scheduler.submit(() -> FileHandler.delete(database, collection, identifier));
    }

    public Map<String, String> getData(){
        return data.asMap();
    }

    public Map<String, String> getVersions(){
        return versions.asMap();
    }

    public String add(String identifier, String json) {
        if(RoseDB.versioning && this.data.get(identifier) != null) {
            this.versions.put(identifier, this.data.get(identifier));
        }

        this.data.put(identifier, json);
        FileHandler.write(database, collection, identifier, json);
        return json;
    }

    public Optional<String> get(String identifier) {
        return Optional.ofNullable(data.get(identifier));
    }

}
