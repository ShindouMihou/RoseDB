package pw.mihou.rosedb.io;

import ch.qos.logback.classic.Level;
import org.apache.commons.io.FilenameUtils;
import pw.mihou.rosedb.RoseDB;
import pw.mihou.rosedb.enums.Levels;
import pw.mihou.rosedb.io.readers.RoseReader;
import pw.mihou.rosedb.io.writers.RoseWriter;
import pw.mihou.rosedb.manager.RoseCollections;
import pw.mihou.rosedb.manager.RoseDatabase;
import pw.mihou.rosedb.utility.Terminal;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class FileHandler {

    public static final FilenameFilter filter = (dir, name) -> name.endsWith(".rose");
    public static String directory;

    /**
     * Sets the default directory path of the database.
     *
     * @param dir the default directory path.
     */
    public static void setDirectory(String dir) {
        directory = dir;
    }

    /**
     * Formats the parameters into their correct path.
     *
     * @param database The database name.
     * @param collection The collection name.
     * @param identifier The identifier name of the item.
     * @return The full path to the item.
     */
    public static String format(String database, String collection, String identifier) {
        return directory + File.separator + database + File.separator + collection + File.separator + identifier + ".rose";
    }

    /**
     * Formats the parameters into their correct path.
     *
     * @param database The database name.
     * @param collection The collection name.
     * @return The full path to the collection.
     */
    public static String format(String database, String collection) {
        return directory + File.separator + database + File.separator + collection;
    }

    /**
     * Formats the parameter into its correct path.
     *
     * @param database The database name.
     * @return The full path to the database.
     */
    public static String format(String database) {
        return directory + File.separator + database;
    }

    /**
     * Makes all the directories to the file.
     *
     * @param path The full path (including name and extension) of the file.
     */
    public static void mkdirs(String path){
        if(!FilenameUtils.getFullPath(path).isEmpty() && !new File(FilenameUtils.getFullPath(path)).exists()) {
            if (!new File(FilenameUtils.getFullPath(path)).mkdirs()) {
                Terminal.setLoggingLevel(Level.ERROR);
                Terminal.log(Levels.ERROR, "Failed to create folders {}, possibly we do not have permission to write.", FilenameUtils.getFullPath(path));
            }
        }
    }

    /**
     * Reads an entire collection and all the items inside the collection.
     *
     * @param database The database that holds the collection.
     * @param collection The name of the collection.
     * @return The entire collection and its items.
     */
    public static CompletableFuture<RoseCollections> readCollection(String database, String collection) {
        return CompletableFuture.supplyAsync(() -> {
            String location = format(database, collection);

            if (!new File(location).exists()) {
                boolean mkdirs = new File(location).mkdirs();
                if (!mkdirs) {
                    Terminal.setLoggingLevel(Level.ERROR);
                    Terminal.log(Levels.ERROR, "Failed to create folders for " + location + ", possibly we do not have permission to write.");
                    return new RoseCollections(collection, database);
                }
            }

            File[] contents = new File(location).listFiles(filter);
            RoseCollections collections = new RoseCollections(collection, database);

            if (contents != null) {
                Arrays.stream(contents).forEach(file -> collections.cache(FilenameUtils.getBaseName(file.getName()), RoseReader.readGZIP(file.getPath()).join()));
            }
            return collections;
        }, Scheduler.getExecutorService());
    }

    /**
     * Reads a backup version if it exists, or else returns empty.
     *
     * @param database The database where the backup is from.
     * @param collection The collection where the backup is from.
     * @param identifier The identifier of the item to be read.
     * @return The older version of the item, if it exists.
     */
    public static Optional<String> readVersion(String database, String collection, String identifier) {
        String location = ".rose_versions" + File.separator + database + File.separator + collection +
                File.separator + identifier + ".rose";

        if (!new File(location).exists()) {
            return Optional.empty();
        }

        return Optional.of(RoseReader.readGZIP(new File(location).getPath()).join());
    }

    /**
     * Reads an entire database including its collections and items.
     *
     * @param database The database to read.
     * @return An entire database with its collections (that contains all the items).
     */
    public static CompletableFuture<RoseDatabase> readDatabase(String database) {
        return CompletableFuture.supplyAsync(() -> {
            String location = format(database);
            if (!new File(location).exists()) {
                boolean mkdirs = new File(location).mkdirs();
                if (!mkdirs) {
                    Terminal.setLoggingLevel(Level.ERROR);
                    Terminal.log(Levels.ERROR, "Failed to create folders for " + location + ", possibly we do not have permission to write.");
                    return new RoseDatabase(database);
                }
            }

            RoseDatabase data = new RoseDatabase(database);
            File[] contents = new File(location).listFiles();

            if (contents != null) {
                Arrays.stream(contents).filter(File::isDirectory)
                        .forEachOrdered(file -> data.cache(FilenameUtils.getBaseName(file.getName()),
                                readCollection(database, FilenameUtils.getBaseName(file.getName())).join()));
            }

            return data;
        }, Scheduler.getExecutorService());
    }

    /**
     * Reads an independent item from a collection if it exists.
     *
     * @param database The database where the item is located.
     * @param collection The collection where the item is located.
     * @param identifier The item's identifier.
     * @return The value of the item, if it exists.
     */
    public static Optional<String> readData(String database, String collection, String identifier) {
        Terminal.log(Levels.DEBUG, "Attempting to read {}/{}/{}.rose", database, collection, identifier);
        String location = format(database, collection, identifier);
        if (!new File(location).exists())
            return Optional.empty();

        return Optional.of(RoseReader.readGZIP(location).join());
    }

    /**
     * Preloads all the databases, collections and items inside the directory.
     */
    public static void preloadAll() {
        File[] contents = new File(RoseDB.directory).listFiles();

        if (contents != null) {
            Arrays.stream(contents).filter(File::isDirectory)
                    .forEachOrdered(file -> RoseDatabase.getDatabase(FilenameUtils.getBaseName(file.getName())));
        }
    }

    /**
     * Migrates all older versions of Rose (that used raw JSON as value)
     * to the newer format which wraps the JSON in GZIP for smaller size.
     */
    public static void migrateAll() {
        File[] contents = new File(directory).listFiles();

        if (contents != null) {
            Arrays.stream(contents).filter(File::isDirectory).forEachOrdered(file -> {
                File[] c = new File(format(FilenameUtils.getBaseName(file.getName()))).listFiles();

                if (c != null) {
                    Arrays.stream(c).filter(File::isDirectory)
                            .forEachOrdered(d -> migrateCollection(FilenameUtils.getBaseName(file.getName()),
                                    FilenameUtils.getBaseName(d.getName())));
                }
            });
        }
    }

    /**
     * Migrates an entire collection's items from raw JSON values
     * to Gzipped JSON values.
     *
     * @param database The database where the collection is located.
     * @param collection The name of the collection.
     */
    public static void migrateCollection(String database, String collection) {
        CompletableFuture.runAsync(() -> {
            Terminal.log(Levels.INFO, "Attempting to migrate " + collection + " from " + database + " to newer format.");
            File[] contents = new File(format(database, collection)).listFiles(filter);

            if (contents != null) {
                Arrays.stream(contents).map(File::getPath)
                .forEachOrdered(path -> RoseReader.read(path).thenAccept(s -> {
                    if (s != null && s.isEmpty() && s.isBlank()) {
                        RoseWriter.writeGZIP(path, s);
                    }
                }).exceptionally(throwable -> {
                    if (throwable != null) {
                        Terminal.log.error("An error occurred while trying to write on {} this usually happens if you are trying to migrate from" +
                                " v1.1 to > v1.2 version with > v1.2 version data, please move the Database folder first before retrying then after configuration is updated, " +
                                "you may add the Database folder again.", path);
                        Terminal.log.error(throwable.getMessage());
                    }
                    return null;
                }));
            }
        });
    }

}
