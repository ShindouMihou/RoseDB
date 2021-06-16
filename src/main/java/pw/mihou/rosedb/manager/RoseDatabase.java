package pw.mihou.rosedb.manager;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.apache.commons.io.FileUtils;
import pw.mihou.rosedb.RoseDB;
import pw.mihou.rosedb.io.FileHandler;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

public class RoseDatabase {

    private final String database;
    private final LoadingCache<String, RoseCollections> rosy;

    public RoseDatabase(String database) {
        this.database = database;
        this.rosy = Caffeine.newBuilder()
                .build(key -> FileHandler.readCollection(database, key).join());
    }

    public void cache(String collection, RoseCollections collections) {
        this.rosy.put(collection, collections);
    }

    public Collection<RoseCollections> getCollections(){
        return rosy.asMap().values();
    }

    public RoseCollections getCollection(String collection) {
        return rosy.get(collection);
    }

    public String getDatabase(){
        return database;
    }

    public void removeCollection(String collection) throws IOException {
        FileUtils.deleteDirectory(new File(RoseDB.directory + "/" + database + "/" + collection + "/"));
        this.rosy.invalidate(collection);
    }

}
