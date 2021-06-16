package pw.mihou.rosedb.io;

import ch.qos.logback.classic.Level;
import org.apache.commons.io.FilenameUtils;
import pw.mihou.rosedb.RoseDB;
import pw.mihou.rosedb.connections.RoseServer;
import pw.mihou.rosedb.enums.Levels;
import pw.mihou.rosedb.io.entities.RoseRequest;
import pw.mihou.rosedb.manager.RoseCollections;
import pw.mihou.rosedb.manager.RoseDatabase;
import pw.mihou.rosedb.utility.Pair;
import pw.mihou.rosedb.utility.Terminal;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class FileHandler {

    private static final ConcurrentLinkedQueue<RoseRequest> queue = new ConcurrentLinkedQueue<>();
    private static final AtomicBoolean threadFull = new AtomicBoolean(false);
    private static final FilenameFilter filter = (dir, name) -> name.endsWith(".rose");
    private static String directory;

    public static void setDirectory(String dir) {
        directory = dir;
    }

    public static CompletableFuture<Void> writeToFile(String path, String value) {
        return write(path, value, false);
    }

    public static CompletableFuture<Void> writeGzip(String path, String value) {
        return write(path, value, true);
    }

    public static CompletableFuture<String> read(String path) {
        return read(path, false);
    }

    private static CompletableFuture<String> read(String path, boolean gzip) {
        return CompletableFuture.supplyAsync(() -> {
            try (BufferedReader r = (!gzip ? Files.newBufferedReader(Paths.get(path)) :
                    new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(new File(path)), 65536))))) {
                return r.readLine();
            } catch (IOException e) {
                Terminal.log(Levels.ERROR, e.getMessage());
            }
            return "";
        }, Scheduler.getExecutorService());
    }

    public static CompletableFuture<String> readGzip(String path) {
        return read(path, true);
    }

    public static CompletableFuture<Void> compress(String path) {
        return read(path, false).thenAccept(s -> writeGzip(path, s));
    }

    public static boolean delete(String database, String collection, String identifier) {
        queue.stream().filter(roseRequest -> filter(roseRequest, database, collection, identifier)).forEachOrdered(queue::remove);
        return delete(format(database, collection, identifier));
    }

    private static boolean delete(String path) {
        try {
            return Files.deleteIfExists(Paths.get(path));
        } catch (IOException exception) {
            Terminal.log(Levels.ERROR, "An exception occurred while trying to delete: " + exception.getMessage());
            return false;
        }
    }

    public static String format(String database, String collection, String identifier) {
        return new StringBuilder(directory).append(File.separator).append(database).append(File.separator).append(collection)
                .append(File.separator).append(identifier).append(".rose").toString();
    }

    public static String format(String database, String collection) {
        return new StringBuilder(directory).append(File.separator).append(database).append(File.separator).append(collection).toString();
    }

    public static String format(String database) {
        return new StringBuilder(directory).append(File.separator).append(database).toString();
    }

    private static boolean filter(RoseRequest roseRequest, String database, String collection, String identifier) {
        return (roseRequest.identifier.equalsIgnoreCase(identifier)
                && roseRequest.collection.equalsIgnoreCase(collection)
                && roseRequest.database.equalsIgnoreCase(database));
    }

    public static void write(String database, String collection, String identifier, String json) {
        Terminal.log(Levels.DEBUG, "Added to queue: {}/{}/{}.rose: {}", database, collection, identifier, json);
        queue.add(new RoseRequest(database, collection, identifier, json));
    }

    public static synchronized void write() {
        if(!threadFull.get()) {
            threadFull.set(true);

            while (!queue.isEmpty()) {
                RoseRequest request = queue.poll();
                Terminal.log(Levels.DEBUG, "Writing {}/{}/{}.rose: {}", request.database, request.collection, request.identifier, request.json);
                writeGzip(format(request.database, request.collection, request.identifier), request.json).join();
            }

            threadFull.set(false);
        }
    }

    private static synchronized CompletableFuture<Void> write(String path, String value, boolean gzip) {
        return CompletableFuture.runAsync(() -> {
            try {
                if (!gzip) {
                    Files.writeString(Paths.get(path), value, StandardCharsets.UTF_8);
                } else {
                    try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(new File(path)), 65536)))) {
                        writer.write(value);
                    }
                }
            } catch (IOException e) {
                Terminal.log(Levels.ERROR, e.getMessage());
            }
        });
    }

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
                Arrays.stream(contents).forEach(file -> collections.cache(FilenameUtils.getBaseName(file.getName()), readGzip(file.getPath()).join()));
            }
            return collections;
        }, Scheduler.getExecutorService());
    }

    public static Optional<String> readVersion(String database, String collection, String identifier) {
        String location = new StringBuilder(".rose_versions")
                .append(File.separator)
                .append(database)
                .append(File.separator).append(collection)
                .append(File.separator).append(identifier)
                .append(".rose").toString();

        if (!new File(location).exists()) {
            return Optional.empty();
        }

        return Optional.of(readGzip(new File(location).getPath()).join());
    }

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

    public static Optional<String> readData(String database, String collection, String identifier) {
        Terminal.log(Levels.DEBUG, "Attempting to read {}/{}/{}.rose", database, collection, identifier);
        String location = format(database, collection, identifier);
        if (!new File(location).exists())
            return Optional.empty();

        return Optional.of(readGzip(location).join());
    }

    public static CompletableFuture<Void> preloadAll() {
        return CompletableFuture.runAsync(() -> {
            File[] contents = new File(RoseDB.directory).listFiles();

            if (contents != null) {
                Arrays.stream(contents).filter(File::isDirectory)
                        .forEachOrdered(file -> RoseServer.getDatabase(FilenameUtils.getBaseName(file.getName())));
            }
        }, Scheduler.getExecutorService());
    }

    public static CompletableFuture<Void> migrateAll() {
        return CompletableFuture.runAsync(() -> {
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
        }, Scheduler.getExecutorService());
    }

    public static CompletableFuture<Void> migrateCollection(String database, String collection) {
        return CompletableFuture.runAsync(() -> {
            Terminal.log(Levels.INFO, "Attempting to migrate " + collection + " from " + database + " to newer format.");
            File[] contents = new File(format(database, collection)).listFiles(filter);

            if (contents != null) {
                Arrays.stream(contents).forEach(file -> compress(file.getPath()));
            }
        });
    }

}
