package pw.mihou.rosedb.manager;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import pw.mihou.rosedb.RoseDB;
import pw.mihou.rosedb.io.FileHandler;
import pw.mihou.rosedb.io.Scheduler;
import pw.mihou.rosedb.io.deleter.RoseDeleter;
import pw.mihou.rosedb.io.writers.RoseWriter;

import java.io.File;
import java.util.Collection;
import java.util.stream.Collectors;

public class RoseDatabase {

    private final String database;
    private final LoadingCache<String, RoseCollections> rosy;
    public static final LoadingCache<String, RoseDatabase> databases = Caffeine.newBuilder()
            .build(key -> FileHandler.readDatabase(key.toLowerCase()).join());

    public RoseDatabase(String database) {
        this.database = database;
        this.rosy = Caffeine.newBuilder()
                .build(key -> FileHandler.readCollection(database, key).join());
    }

    /**
     * Places a collection inside the cache.
     *
     * @param collection The name of the collection.
     * @param collections The actual Collections object.
     */
    public void cache(String collection, RoseCollections collections) {
        this.rosy.put(collection, collections);
    }

    /**
     * Gets all the collections inside the database.
     *
     * @return All the collection inside the database.
     */
    public Collection<RoseCollections> getCollections(){
        return rosy.asMap().values();
    }

    /**
     * Gets the collection if it exists, or else makes one.
     *
     * @param collection The collection name to get.
     * @return The requested collection.
     */
    public RoseCollections getCollection(String collection) {
        return rosy.get(collection);
    }

    /**
     * Gets the name of the database.
     *
     * @return The name of the database.
     */
    public String getDatabase(){
        return database;
    }

    /**
     * Removes a collection inside the database.
     *
     * @param collection The name of the collection to remove.
     */
    public void removeCollection(String collection) {
        this.rosy.invalidate(collection);
        RoseWriter.queue.removeAll(RoseWriter.queue.stream().filter(r -> r.collection.equals(collection))
                .collect(Collectors.toUnmodifiableList()));
        Scheduler.submit(() -> RoseDeleter.deleteDirectory(new File(RoseDB.directory + "/" + database + "/" + collection + "/"), 0));
    }

    /**
     * Gets the database, if it exists or else creates a new one.
     *
     * @param db The database name to get.
     * @return the database requested.
     */
    public static RoseDatabase getDatabase(String db) {
        return databases.get(db.toLowerCase());
    }

    /**
     * Removes the database and its items.
     *
     * @param db The database name to remove.
     */
    public static synchronized void removeDatabase(String db) {
        databases.invalidate(db.toLowerCase());
        RoseWriter.queue.removeAll(RoseWriter.queue.stream().filter(r -> r.database.equals(db)).collect(Collectors.toUnmodifiableList()));
        Scheduler.submit(() -> RoseDeleter.deleteDirectory(new File(String.format(RoseDB.directory + "/%s/", db)), 0));
    }

}
