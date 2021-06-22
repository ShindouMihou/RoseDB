package pw.mihou.rosedb.manager;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import pw.mihou.rosedb.RoseDB;
import pw.mihou.rosedb.io.FileHandler;
import pw.mihou.rosedb.io.Scheduler;
import pw.mihou.rosedb.io.deleter.RoseDeleter;
import pw.mihou.rosedb.io.writers.RoseWriter;
import pw.mihou.rosedb.utility.Pair;

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
    public Pair<Boolean, String> removeCollection(String collection) {
        if(RoseDeleter.cleanJournal(roseRequest -> roseRequest.database.equals(database) && roseRequest.collection.equals(collection))) {
            Pair<Boolean, String> val = RoseDeleter.deleteDirectory(new File(RoseDB.directory + File.separator + database + File.separator + collection + File.separator));
            if(val.getLeft())
                this.rosy.invalidate(collection);

            return val;
        }

        return Pair.of(false, "Failed to clean journal for the collection.");
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
    public static synchronized Pair<Boolean, String> removeDatabase(String db) {
        if(RoseDeleter.cleanJournal(roseRequest -> roseRequest.database.equals(db))) {
            Pair<Boolean, String> val = RoseDeleter.deleteDirectory(new File(RoseDB.directory+File.separator +db+File.separator));
            if (val.getLeft())
                databases.invalidate(db.toLowerCase());

            return val;
        }

        return Pair.of(false, "Failed to clean journal for the database.");
    }

}
