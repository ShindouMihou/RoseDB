package pw.mihou.rosedb.io;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONObject;
import pw.mihou.rosedb.RoseDB;
import pw.mihou.rosedb.enums.Levels;
import pw.mihou.rosedb.io.entities.RoseEntity;
import pw.mihou.rosedb.io.entities.RoseRequest;
import pw.mihou.rosedb.manager.RoseCollections;
import pw.mihou.rosedb.manager.RoseDatabase;
import pw.mihou.rosedb.utility.Terminal;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

public class FileHandler {

    private static final ConcurrentLinkedQueue<RoseRequest> queue = new ConcurrentLinkedQueue<>();

    private static final FilenameFilter filter = (dir, name) -> name.toLowerCase().endsWith(".rose") && dir.isFile();

    public static CompletableFuture<String> read(String path){
        return CompletableFuture.supplyAsync(() -> {
            StringBuilder builder = new StringBuilder();
            try {
                final BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(path), StandardCharsets.UTF_8)); String line;
                while((line = in.readLine()) != null){
                    builder.append(line);
                }
                in.close();
            } catch (final IOException e){
                Terminal.log(Levels.ERROR, e.getMessage());
            }
            return builder.toString();
        });
    }

    public static void write(String database, String collection, String identifier, String json){
        if(queue.isEmpty()) {
            writeDataAsJson(new RoseRequest(database, collection, identifier, json));
        } else {
            queue.add(new RoseRequest(database, collection, identifier, json));
        }
    }

    public static boolean delete(String database, String collection, String identifier){
        queue.stream().filter(roseRequest -> roseRequest.identifier.equalsIgnoreCase(identifier)).forEachOrdered(queue::remove);

        return FileUtils.deleteQuietly(new File(String.format(RoseDB.directory+"/%s/%s/%s.rose", database, collection, identifier)));
    }

    private static void writeDataAsJson(RoseRequest request){
        File file = new File(String.format(RoseDB.directory+"/%s/%s/%s.rose", request.database, request.collection, request.identifier));

        if(!file.exists()) {
            writeToFile(String.format(RoseDB.directory + "/%s/%s/%s.rose", request.database, request.collection, request.identifier), request.json);
        }

            read(String.format(RoseDB.directory+"/%s/%s/%s.rose", request.database, request.collection, request.identifier))
                    .thenAccept(s -> writeToFile(String.format(RoseDB.directory + "/%s/%s/%s.rose", request.database,
                            request.collection, request.identifier), request.json));

        if(!queue.isEmpty())
            writeDataAsJson(queue.poll());
    }

    public static CompletableFuture<Void> writeToFile(String path, String value){
        return CompletableFuture.runAsync(() -> { try { Files.write(Paths.get(path), value.getBytes()); } catch (IOException e){ Terminal.log(Levels.ERROR, e.getMessage()); } }); }

    public static RoseCollections readCollection(String database, String collection){
        if(!new File(String.format(RoseDB.directory+"/%s/%s", database, collection)).exists())
            new File(String.format(RoseDB.directory + "/%s/%s", database, collection)).mkdirs();

        File[] contents = new File(String.format(RoseDB.directory+"/%s/%s", database, collection)).listFiles(filter);
        RoseCollections collections = new RoseCollections(collection, database);

        if(contents != null) {
            for (File content : contents) {
                read(content.getPath()).thenAccept(s -> collections.cache(FilenameUtils.getBaseName(content.getName()), s));
            }
        }
        return collections;
    }

    public static RoseDatabase readDatabase(String database){
        if(!new File(String.format(RoseDB.directory+"/%s", database)).exists())
            new File(String.format(RoseDB.directory + "/%s", database)).mkdirs();

        File[] contents = new File(String.format(RoseDB.directory+"/%s", database)).listFiles();
        RoseDatabase data = new RoseDatabase(database);

        if(contents != null) {
            for (File content : contents) {
                if (content.isDirectory())
                    data.cache(FilenameUtils.getBaseName(content.getName()), readCollection(FilenameUtils.getBaseName(content.getName()), database));
            }
        }

        return data;
    }

    public static Optional<RoseEntity> readData(String database, String collection, String identifier){
        if(!new File(String.format(RoseDB.directory+"/%s/%s/%s.rose", database, collection, identifier)).exists())
            return Optional.empty();

        return Optional.of(new RoseEntity(read(String.format(RoseDB.directory+"/%s/%s/%s.rose", database, collection, identifier)).join()));
    }

}
