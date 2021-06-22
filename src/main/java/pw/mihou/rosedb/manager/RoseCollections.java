package pw.mihou.rosedb.manager;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import journal.io.api.Journal;
import org.json.JSONObject;
import pw.mihou.rosedb.RoseDB;
import pw.mihou.rosedb.enums.Levels;
import pw.mihou.rosedb.io.FileHandler;
import pw.mihou.rosedb.io.Scheduler;
import pw.mihou.rosedb.io.deleter.RoseDeleter;
import pw.mihou.rosedb.utility.Pair;
import pw.mihou.rosedb.utility.Terminal;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
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
        Pair<Integer, String> response = add(identifier, json);
        if(response.getLeft() == 1) {
            versions.invalidate(identifier);
        }

        return new Pair<>(response.getLeft() == 1, response.getRight());
    }

    /**
     * Deletes an item from the collection.
     *
     * @param identifier The identifier of the item.
     */
    public Pair<Boolean, String> delete(String identifier) {
        Pair<Boolean, String> val = RoseDeleter.delete(database, collection, identifier);
        if(val.getLeft())
            this.data.invalidate(identifier);
        return val;
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
    public Pair<Integer, String> add(String identifier, String json) {
        if(RoseDB.versioning && this.data.get(identifier) != null) {
            this.versions.put(identifier, this.data.get(identifier));
        }

        // We are trying to make sure that we should only respond if the data we have is saved on journal.
        // because once the data is saved on the journal, it is guranteed to be written even on failure.
        Pair<Integer, String> response;
        try {
            response  = RoseDB.timeLimiter.executeFutureSupplier(() -> CompletableFuture.supplyAsync(() -> {
                try {
                    this.data.put(identifier, json);
                    // We send our write request to the journal for durability.
                    RoseDB.journal.write(new JSONObject().put("database", database).put("collection", collection)
                            .put("identifier", identifier).put("json", json).toString().getBytes(), Journal.WriteType.SYNC);
                    return new Pair<>(1, json);
                } catch (IOException exception) {
                    Terminal.log(Levels.ERROR, "We were unable to write to journal: {}", exception.getMessage());
                    return new Pair<>(0, exception.getMessage());
                }
            }));
        } catch (Exception exception) {
            Terminal.log(Levels.ERROR, "Unable to fulfill add request: {}", exception.getMessage());
            return new Pair<>(0, exception.getMessage());
        }

        return response;
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
