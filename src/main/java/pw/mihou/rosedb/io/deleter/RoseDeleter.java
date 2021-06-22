package pw.mihou.rosedb.io.deleter;

import journal.io.api.Journal;
import journal.io.api.Location;
import org.apache.commons.io.FileUtils;
import pw.mihou.rosedb.RoseDB;
import pw.mihou.rosedb.enums.Levels;
import pw.mihou.rosedb.io.FileHandler;
import pw.mihou.rosedb.io.entities.RoseRequest;
import pw.mihou.rosedb.io.writers.RoseWriter;
import pw.mihou.rosedb.utility.Pair;
import pw.mihou.rosedb.utility.Terminal;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.function.Predicate;

public class RoseDeleter {

    public static Pair<Boolean, String> delete(String database, String collection, String identifier){
        try {
            for (Location location : RoseDB.journal.redo()) {
                RoseRequest request = RoseDB.gson.fromJson(new String(RoseDB.journal.read(location, Journal.ReadType.SYNC)), RoseRequest.class);
                if(filter(request, database, collection, identifier))
                    RoseDB.journal.delete(location);
            }
            RoseDB.journal.sync();
            RoseDB.journal.compact();
            return delete(FileHandler.format(database, collection, identifier));
        } catch (IOException exception) {
            Terminal.log(Levels.ERROR, "Rose Writer has returned an exception : {}", exception.getMessage());
            return Pair.of(false, exception.getMessage());
        }
    }

    public static boolean cleanJournal(Predicate<RoseRequest> predicate){
        try {
            for (Location location : RoseDB.journal.redo()) {
                RoseRequest request = RoseDB.gson.fromJson(new String(RoseDB.journal.read(location, Journal.ReadType.SYNC)), RoseRequest.class);
                if(predicate.test(request))
                    RoseDB.journal.delete(location);
            }
            RoseDB.journal.sync();
            RoseDB.journal.compact();
            return true;
        } catch (IOException exception) {
            Terminal.log(Levels.ERROR, "Rose Writer has returned an exception : {}", exception.getMessage());
            return false;
        }
    }

    public static Pair<Boolean, String> deleteDirectory(File directory){
        try {
            FileUtils.deleteDirectory(directory);
            return Pair.of(true, "The directory was successfully removed.");
        } catch (IOException exception) {
            Terminal.log(Levels.DEBUG, "Attempt to delete file ({}) was stopped by an IOException ({}), retrying attempt...", directory.getPath(), exception.getMessage());
            try {
                return RoseDB.retry.executeCallable(() -> deleteDirectory(directory));
            } catch (Exception e) {
                Terminal.log(Levels.ERROR, "An exception occurred while trying to delete: {}", e.getMessage());
                return Pair.of(false, exception.getMessage());
            }
        } catch (IllegalArgumentException e) {
            Terminal.log(Levels.ERROR, "Attempt to delete file ({}) was stopped by IllegalArgumentException ({}).", directory.getPath(), e.getMessage());
            return Pair.of(false, e.getMessage());
        }
    }

    public static Pair<Boolean, String> delete(String path){
        try {
            boolean val = Files.deleteIfExists(Paths.get(path));
            return Pair.of(val, val ? "The file/directory was deleted successfully." : "Attempt to delete file or directory was stopped, " +
                    "either because the file didn't exist or external reasons.");
        } catch (IOException e){
            Terminal.log(Levels.DEBUG, "Attempt to delete file ({}) was stopped by an IOException ({}), retrying attempt...", path, e.getMessage());
            try {
                return RoseDB.retry.executeCallable(() -> delete(path));
            } catch (Exception exception) {
                Terminal.log(Levels.ERROR, "An exception occurred while trying to delete: {}", exception.getMessage());
                return Pair.of(false, exception.getMessage());
            }
        }
    }

    private static boolean filter(RoseRequest roseRequest, String database, String collection, String identifier) {
        return (roseRequest.identifier.equalsIgnoreCase(identifier)
                && roseRequest.collection.equalsIgnoreCase(collection)
                && roseRequest.database.equalsIgnoreCase(database));
    }

}
