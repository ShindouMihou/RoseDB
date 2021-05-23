package pw.mihou.rosedb.io;

import org.apache.commons.io.FilenameUtils;
import pw.mihou.rosedb.RoseDB;
import pw.mihou.rosedb.connections.RoseServer;
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
import java.util.concurrent.atomic.AtomicBoolean;

public class FileHandler {

    private static final ConcurrentLinkedQueue<RoseRequest> queue = new ConcurrentLinkedQueue<>();
    private static final AtomicBoolean threadFull = new AtomicBoolean(false);
    private static final FilenameFilter filter = (dir, name) -> name.endsWith(".rose");

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
        // This way, it actually queues the requests.
        queue.add(new RoseRequest(database, collection, identifier, json));

        // We are using == 1 since we are already adding one to the queue.
        // The reason why we are doing the stream as well is because we don't want to delay the other requests.
        if (queue.stream().filter(roseRequest -> (roseRequest.identifier.equalsIgnoreCase(identifier) && roseRequest.collection.equalsIgnoreCase(collection) && roseRequest.database.equalsIgnoreCase(database))).count() == 1) {
            write();
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

    public static void executeFinalRuntime(){
        if(!queue.isEmpty()){
            Terminal.log(Levels.DEBUG, "Executing final thread to finish remaining " + queue.size() + " write requests.");
            write();
        }
    }

    private static void write() {
        if(!threadFull.get()) {
            Scheduler.getExecutorService().submit(() -> {
                if (!queue.isEmpty()) {
                    threadFull.set(true);
                    // We will be polling here instead.
                    RoseRequest request = queue.poll();
                    String location = new StringBuilder(RoseDB.directory).append(File.separator).append(request.database).append(File.separator).append(request.collection)
                            .append(File.separator).append(request.identifier).append(".rose").toString();

                    if (!new File(location).exists()) {
                        writeToFile(location, request.json).join();
                    } else {
                        read(location).thenAccept(s -> writeToFile(location, request.json)).join();
                    }
                    threadFull.set(false);
                    if (!queue.isEmpty()) {
                        write();
                    }
                }
            });
        }
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

    public static CompletableFuture<RoseCollections> readCollection(String database, String collection) {
        return CompletableFuture.supplyAsync(() -> {
            String location = new StringBuilder(RoseDB.directory).append(File.separator).append(database).append(File.separator).append(collection).toString();
            if (!new File(location).exists()) {
                boolean mkdirs = new File(location).mkdirs();
                if(!mkdirs){
                    Terminal.setLoggingLevel(Levels.ERROR);
                    Terminal.log(Levels.ERROR, "Failed to create folders for " + location + ", possibly we do not have permission to write.");
                    return new RoseCollections(collection, database);
                }
            }


            File[] contents = new File(location).listFiles(filter);
            RoseCollections collections = new RoseCollections(collection, database);

            if (contents != null) {
                Arrays.stream(contents).forEach(file -> collections.cache(FilenameUtils.getBaseName(file.getName()), read(file.getPath()).join()));
            }
            return collections;
        });
    }

    public static CompletableFuture<RoseDatabase> readDatabase(String database) {
        return CompletableFuture.supplyAsync(() -> {
            String location = new StringBuilder(RoseDB.directory).append(File.separator).append(database).toString();
            if (!new File(location).exists()) {
                boolean mkdirs = new File(location).mkdirs();
                if(!mkdirs){
                    Terminal.setLoggingLevel(Levels.ERROR);
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
        });
    }

    public static Optional<String> readData(String database, String collection, String identifier) {
        String location = new StringBuilder(RoseDB.directory).append(File.separator).append(database).append(File.separator).append(collection)
                .append(File.separator).append(identifier).append(".rose").toString();
        if (!new File(location).exists())
            return Optional.empty();

        return Optional.of(read(location).join());
    }

    public static CompletableFuture<Void> preloadAll(){
        return CompletableFuture.runAsync(() -> {
            File[] contents = new File(RoseDB.directory).listFiles();

            if (contents != null) {
                Arrays.stream(contents).filter(File::isDirectory)
                        .forEachOrdered(file -> RoseServer.getDatabase(FilenameUtils.getBaseName(file.getName())));
            }
        });
    }

}
