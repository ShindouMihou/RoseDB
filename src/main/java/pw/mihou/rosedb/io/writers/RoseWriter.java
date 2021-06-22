package pw.mihou.rosedb.io.writers;

import journal.io.api.Journal;
import journal.io.api.Location;
import pw.mihou.rosedb.RoseDB;
import pw.mihou.rosedb.connections.RoseServer;
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

    public static void writeGZIP(String path, String value) {
        write(path, value, true);
    }

    public static void write(String path, String value) {
        write(path, value, false).join();
    }

    public static void write(String database, String collection, String identifier, String json) {
        Terminal.log(Levels.DEBUG, "Added to write queue: {}/{}/{}.rose with value {}", database, collection, identifier, json);
        queue.add(new RoseRequest(database, collection, identifier, json));
    }

    public static synchronized void write() {
        if (activeThread.get())
            return;

        activeThread.set(true);
        try {
            for (Location location : RoseDB.journal.redo()) {
                RoseRequest request = RoseDB.gson.fromJson(new String(RoseDB.journal.read(location, Journal.ReadType.SYNC)), RoseRequest.class);
                Terminal.log(Levels.DEBUG, "Request to write on {}/{}/{}.rose with value {} sent to writer.", request.database, request.collection,
                        request.identifier, request.json);
                RoseDB.journal.delete(location);
                write(FileHandler.format(request.database, request.collection, request.identifier), request.json, true).join();
            }
            RoseDB.journal.sync();
            RoseDB.journal.compact();

            // We can also add the queue writer since collection adds are now done via journal.
            while (!queue.isEmpty()) {
                RoseRequest request = queue.poll();
                Terminal.log(Levels.DEBUG, "Request to write on {}/{}/{}.rose with value {} sent to writer.", request.database, request.collection,
                        request.identifier, request.json);
                if (queue.peek() != null && request.compare(queue.peek()))
                    write(FileHandler.format(request.database, request.collection, request.identifier), request.json, true).join();
                else
                    write(FileHandler.format(request.database, request.collection, request.identifier), request.json, true);
            }

            if(!RoseServer.isOpen) {
                try {
                    RoseDB.journal.close();
                } catch (IOException exception) {
                    Terminal.log(Levels.ERROR, "Attempt to close journal was met with {}", exception.getMessage());
                }
            }
        } catch (IOException exception) {
            Terminal.log(Levels.ERROR, "Rose Writer has returned an exception : {}", exception.getMessage());
        }
        activeThread.set(false);
    }

    private static synchronized CompletableFuture<Void> write(String path, String value, boolean gzip) {
        return CompletableFuture.runAsync(() -> {
            FileHandler.mkdirs(path);
            try {
                try (BufferedWriter writer = (!gzip ? Files.newBufferedWriter(new File(path).toPath()) :
                        new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream((new FileOutputStream((new File(path)))), 8192))))) {
                    InternalLocks.lock(path);
                    Terminal.log(Levels.DEBUG, "Attempting to write on {} with {} value.", path, value);
                    writer.write(value);
                } catch (InternalLocks.FileLockedException e) {
                    Terminal.log(Levels.DEBUG, "Lock for {} was already acquired by JVM, thread will retry in awhile.", path);
                    RoseDB.retry.executeRunnable(() -> write(path, value, gzip));
                } finally {
                    InternalLocks.release(path);
                }
            } catch (IOException e) {
                Terminal.log(Levels.ERROR, "An error occurred while trying to write {}: {}", path, e.getMessage());
            }
        }, Scheduler.getExecutorService());
    }

}
