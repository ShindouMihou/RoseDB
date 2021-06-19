package pw.mihou.rosedb.io.readers;

import pw.mihou.rosedb.enums.Levels;
import pw.mihou.rosedb.io.Scheduler;
import pw.mihou.rosedb.io.internals.InternalLocks;
import pw.mihou.rosedb.utility.Terminal;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class RoseReader {

    public synchronized static CompletableFuture<String> readGZIP(String path){
        return CompletableFuture.supplyAsync(() -> read(path, true, 0), Scheduler.getExecutorService());
    }

    public synchronized static CompletableFuture<String> read(String path){
        return CompletableFuture.supplyAsync(() -> read(path, false, 0), Scheduler.getExecutorService());
    }

    private synchronized static String read(String path, boolean gzip, int fails){
        if(!new File(path).exists() || !new File(path).canRead()){
            Terminal.log(Levels.ERROR, "An error occurred while trying to read {}, we either don't have permission to read file or the file does not exist.");
            return null;
        }

        try {
            try (BufferedReader reader = (!gzip ? Files.newBufferedReader(Paths.get(path)) :
                    new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(new File(path)), 65536))))) {
                InternalLocks.lockRead(path);
                return reader.lines().collect(Collectors.joining("\n"));
            } catch (InternalLocks.FileLockedException e) {
                if(fails < 100) {
                    Terminal.log(Levels.DEBUG, "Lock for {} was already acquired by JVM, thread will retry in awhile.", path);
                    Thread.sleep(50);
                    return read(path, gzip, fails + 1);
                } else {
                    Terminal.log(Levels.ERROR, "Unable to retrieve lock for {} after interval time: {}", path, e.getMessage());
                    return null;
                }
            } finally {
                InternalLocks.releaseRead(path);
            }
        } catch (IOException | InterruptedException e){
            Terminal.log(Levels.ERROR, "An error occurred while trying to write {}: {}", path, e.getMessage());
        }
        return null;
    }

}
