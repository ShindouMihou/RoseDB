package pw.mihou.rosedb.manager;

import org.apache.commons.io.FileUtils;
import pw.mihou.rosedb.RoseDB;
import pw.mihou.rosedb.io.FileHandler;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RoseDatabase {

    private final String database;
    private final Map<String, RoseCollections> rosy = new ConcurrentHashMap<>();

    public RoseDatabase(String database){
        this.database = database;
    }

    public void cache(String collection, RoseCollections collections){
        this.rosy.put(collection, collections);
    }

    public RoseCollections getCollection(String collection){
        if(!rosy.containsKey(collection))
            rosy.put(collection, FileHandler.readCollection(database, collection));

        return rosy.get(collection);
    }

    public void removeCollection(String collection) throws IOException {
        FileUtils.deleteDirectory(new File(String.format(RoseDB.directory+"/%s/%s/", database, collection)));
        this.rosy.remove(collection);
    }

}
