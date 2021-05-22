package pw.mihou.rosedb.io;

import org.apache.commons.io.FilenameUtils;
import pw.mihou.rosedb.RoseDB;
import pw.mihou.rosedb.enums.Levels;
import pw.mihou.rosedb.io.entities.RoseRequest;
import pw.mihou.rosedb.manager.RoseCollections;
import pw.mihou.rosedb.manager.RoseDatabase;
import pw.mihou.rosedb.utility.Terminal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FileHandler {

    private static final ConcurrentLinkedQueue<RoseRequest> queue = new ConcurrentLinkedQueue<>();

    private static final FilenameFilter filter = (dir, name) -> name.toLowerCase().endsWith(".rose") && dir.isFile();

    public static CompletableFuture<String> read(String path) {
        return CompletableFuture.supplyAsync(() -> {
            try (BufferedReader reader = Files.newBufferedReader(Paths.get(path))) {
                return reader.readLine();
            } catch (final IOException e) {
                Terminal.log(Levels.ERROR, e.getMessage());
            }
            return "";
        }, Scheduler.getExecutorService());
    }

    public static void write(String database, String collection, String identifier, String json) {
        if (queue.isEmpty()) {
            writeDataAsJson(new RoseRequest(database, collection, identifier, json));
        } else {
            queue.add(new RoseRequest(database, collection, identifier, json));
        }
    }

    public static boolean delete(String database, String collection, String identifier) {
        try {
            queue.stream().filter(roseRequest -> roseRequest.identifier.equalsIgnoreCase(identifier)).forEachOrdered(queue::remove);
            return Files.deleteIfExists(Paths.get(new StringBuilder(RoseDB.directory).append(File.separator).append(database).append(File.separator).append(collection)
                    .append(File.separator).append(identifier).append(".rose").toString()));
        } catch (IOException exception) {
            Terminal.log(Levels.ERROR, "An exception occurred while trying to delete: " + exception.getMessage());
        }
        return false;
    }

    private static CompletableFuture<Void> writeDataAsJson(RoseRequest request) {
        return CompletableFuture.runAsync(() -> {
            String location = new StringBuilder(RoseDB.directory).append(File.separator).append(request.database).append(File.separator).append(request.collection)
                    .append(File.separator).append(request.identifier).append(".rose").toString();

            if (!new File(location).exists()) {
                writeToFile(location, request.json);
            } else {
                read(location).thenAccept(s -> writeToFile(location, request.json));
            }

            if (!queue.isEmpty()) {
                writeDataAsJson(queue.poll());
            }
        }, Scheduler.getExecutorService());
    }

    public static CompletableFuture<Void> writeToFile(String path, String value) {
        return CompletableFuture.runAsync(() -> {
            try {
                Files.writeString(Paths.get(path), value, Charset.defaultCharset());
            } catch (IOException e) {
                Terminal.log(Levels.ERROR, e.getMessage());
            }
        }, Scheduler.getExecutorService());
    }

    public static RoseCollections readCollection(String database, String collection) {
        String location = new StringBuilder(RoseDB.directory).append(File.separator).append(database).append(File.separator).append(collection).toString();
        if (!new File(location).exists()) {
            new File(location).mkdirs();
        }


        File[] contents = new File(location).listFiles(filter);
        RoseCollections collections = new RoseCollections(collection, database);

        if (contents != null) {
            Arrays.stream(contents)
                    .forEach(file -> read(file.getPath()).thenAccept(s ->
                            collections.cache(FilenameUtils.getBaseName(file.getName()), s)));
        }
        return collections;
    }

    public static RoseDatabase readDatabase(String database) {
        String location = new StringBuilder(RoseDB.directory).append(File.separator).append(database).toString();
        if (!new File(location).exists()) {
            //noinspection ResultOfMethodCallIgnored
            new File(location).mkdirs();
        }

        RoseDatabase data = new RoseDatabase(database);
        File[] contents = new File(location).listFiles();

        if (contents != null) {
            Arrays.stream(contents).filter(File::isDirectory)
                    .forEachOrdered(file -> data.cache(FilenameUtils.getBaseName(file.getName()),
                            readCollection(database, FilenameUtils.getBaseName(file.getName()))));
        }

        return data;
    }

    public static Optional<String> readData(String database, String collection, String identifier) {
        String location = new StringBuilder(RoseDB.directory).append(File.separator).append(database).append(File.separator).append(collection)
                .append(File.separator).append(identifier).append(".rose").toString();
        if (!new File(location).exists())
            return Optional.empty();

        return Optional.of(read(location).join());
    }

}
