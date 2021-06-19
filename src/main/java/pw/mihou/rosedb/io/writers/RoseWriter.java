package pw.mihou.rosedb.io.writers;

import pw.mihou.rosedb.enums.Levels;
import pw.mihou.rosedb.io.FileHandler;
import pw.mihou.rosedb.io.Scheduler;
import pw.mihou.rosedb.io.entities.RoseRequest;
import pw.mihou.rosedb.io.internals.InternalLocks;
import pw.mihou.rosedb.utility.Terminal;

import java.io.*;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.GZIPOutputStream;

public class RoseWriter {

    public static final ConcurrentLinkedQueue<RoseRequest> queue = new ConcurrentLinkedQueue<>();
    private static final AtomicBoolean activeThread = new AtomicBoolean(false);

    public static void writeGZIP(String path, String value){
        write(path, value, true, 0);
    }

    public static void write(String path, String value){
        write(path, value, false, 0);
    }

    public static void write(String database, String collection, String identifier, String json){
        Terminal.log(Levels.DEBUG, "Added to write queue: {}/{}/{}.rose with value {}", database, collection, identifier, json);
        queue.add(new RoseRequest(database, collection, identifier, json));
    }

    public static synchronized void write(){
        if(activeThread.get())
            return;

        activeThread.set(true);
        while(!queue.isEmpty()){
            RoseRequest request = queue.poll();
            Terminal.log(Levels.DEBUG, "Attempting to write on {}/{}/{}.rose with value {}", request.database, request.collection, request.identifier, request.json);
            write(FileHandler.format(request.database, request.collection, request.identifier), request.json, true, 0);
        }
        activeThread.set(false);
    }

    private static synchronized CompletableFuture<Void> write(String path, String value, boolean gzip, int fails){
        return CompletableFuture.runAsync(() -> {
            FileHandler.mkdirs(path);

            try {
                try (BufferedWriter writer = (!gzip ? Files.newBufferedWriter(new File(path).toPath()) :
                        new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(( new FileOutputStream((new File(path)))), 65536))))) {
                    InternalLocks.lock(path);
                    writer.write(value);
                } catch (InternalLocks.FileLockedException e) {
                    if(fails < 50) {
                        Terminal.log(Levels.DEBUG, "Lock for {} was already acquired by JVM, thread will retry in awhile.", path);
                        Thread.sleep(50);
                        write(path, value, gzip, fails + 1);
                    } else {
                        Terminal.log(Levels.ERROR, "Unable to retrieve lock for {} after interval time: {}", path, e.getMessage());
                    }
                } finally {
                    InternalLocks.release(path);
                }
            } catch (IOException | InterruptedException e){
                Terminal.log(Levels.ERROR, "An error occurred while trying to write {}: {}", path, e.getMessage());
            }
        }, Scheduler.getExecutorService());
    }

}
