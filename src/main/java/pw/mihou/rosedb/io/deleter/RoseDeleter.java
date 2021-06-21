package pw.mihou.rosedb.io.deleter;

import org.apache.commons.io.FileUtils;
import pw.mihou.rosedb.enums.Levels;
import pw.mihou.rosedb.io.FileHandler;
import pw.mihou.rosedb.io.entities.RoseRequest;
import pw.mihou.rosedb.io.writers.RoseWriter;
import pw.mihou.rosedb.utility.Terminal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class RoseDeleter {

    public static void delete(String database, String collection, String identifier){
        RoseWriter.queue.stream().filter(roseRequest -> filter(roseRequest, database, collection, identifier))
                .forEachOrdered(RoseWriter.queue::remove);
        delete(FileHandler.format(database, collection, identifier), 0);
    }

    public static void deleteDirectory(File directory, int fails){
        try {
            FileUtils.deleteDirectory(directory);
        } catch (IOException exception) {
            if(fails < 10) {
                Terminal.log(Levels.ERROR, "Attempt to delete directory file ({}) was stopped by an IOException ({}), retrying attempt...",
                        directory.getPath(),
                        exception.getMessage());
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Terminal.log(Levels.ERROR, "Attempt to delete file ({}) was stopped by InterruptedException ({}), forcing retry.",
                            directory.getPath(),
                            e.getMessage());
                }
                deleteDirectory(directory, fails + 1);
            }
        } catch (IllegalArgumentException e) {
            Terminal.log(Levels.ERROR, "Attempt to delete file ({}) was stopped by IllegalArgumentException ({}).", directory.getPath(), e.getMessage());
        }
    }

    public static void delete(String path, int fails){
        try {
            Files.deleteIfExists(Paths.get(path));
        } catch (IOException e){
            if(fails < 10) {
                Terminal.log(Levels.DEBUG, "Attempt to delete file ({}) was stopped by an IOException ({}), retrying attempt...", path, e.getMessage());
                try {
                    Thread.sleep(500);
                } catch (InterruptedException interruptedException) {
                    Terminal.log(Levels.ERROR, "Attempt to delete file ({}) was stopped by InterruptedException ({}), forcing retry.", path, interruptedException.getMessage());
                }
                delete(path, fails + 1);
            } else {
                Terminal.log(Levels.ERROR, "An exception occurred while trying to delete: {}", e.getMessage());
            }
        }
    }

    private static boolean filter(RoseRequest roseRequest, String database, String collection, String identifier) {
        return (roseRequest.identifier.equalsIgnoreCase(identifier)
                && roseRequest.collection.equalsIgnoreCase(collection)
                && roseRequest.database.equalsIgnoreCase(database));
    }

}
