package pw.mihou.rosedb.manager;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import pw.mihou.rosedb.RoseDB;
import pw.mihou.rosedb.io.FileHandler;
import pw.mihou.rosedb.io.Scheduler;
import pw.mihou.rosedb.io.deleter.RoseDeleter;
import pw.mihou.rosedb.io.writers.RoseWriter;
import pw.mihou.rosedb.utility.Pair;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class RoseCollections {

    private final LoadingCache<String, String> data;
    private LoadingCache<String, String> versions;
    public final String collection;
    private final String database;

    /**
     * Creates a new collection on the database.
     *
     * @param collection The name of the collection.
     * @param database The database where it is being held.
     */
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

    /**
     * Caches an item to the memory.
     *
     * @param identifier The identifier of the item.
     * @param json The item's value which should be in JSON.
     */
    public void cache(String identifier, String json) {
        this.data.put(identifier, json);
    }

    /**
     * Reverts an item a step backwards before a recent change occurred.
     *
     * @param identifier The identifier of the item.
     * @return The old value inside the item.
     */
    public Pair<Boolean, String> revert(String identifier){
        if(versions.get(identifier) == null)
            return new Pair<>(false, "");

        String json = versions.get(identifier);
        versions.invalidate(identifier);

        add(identifier, json);
        return new Pair<>(true, json);
    }

    /**
     * Deletes an item from the collection.
     *
     * @param identifier The identifier of the item.
     */
    public void delete(String identifier) {
        this.data.invalidate(identifier);
        Scheduler.submit(() -> RoseDeleter.delete(database, collection, identifier));
    }

    /**
     * Gets all the items inside the collection,
     *
     * @return All the items inside the collection.
     */
    public Map<String, String> getData(){
        return data.asMap();
    }

    /**
     * Gets all the backup versions of the items inside the collection.
     *
     * @return All the backup versions of the items of the collection.
     */
    public Map<String, String> getVersions(){
        return versions.asMap();
    }

    /**
     * Adds an item to the collection.
     *
     * @param identifier The identifier of the item.
     * @param json The value of the item, should always be in JSON.
     * @return The value of the item.
     */
    public String add(String identifier, String json) {
        if(RoseDB.versioning && this.data.get(identifier) != null) {
            this.versions.put(identifier, this.data.get(identifier));
        }

        this.data.put(identifier, json);
        RoseWriter.write(database, collection, identifier, json);
        return json;
    }

    /**
     * Gets an item from the collection if it exists.
     *
     * @param identifier The identifier of the item.
     * @return The value of the item, if it exists.
     */
    public Optional<String> get(String identifier) {
        return Optional.ofNullable(data.get(identifier));
    }

}
