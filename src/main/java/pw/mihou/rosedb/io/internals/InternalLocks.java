package pw.mihou.rosedb.io.internals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A locking mechanism that is used to internally lock a file.
 * This is used to check if a thread is writing to a file or not.
 *
 * And if a thread is already writing to a file, anything that attempts
 * to read the file will wait until the lock is freed before attempting a read.
 *
 * This does not prevent other processes from accessing our files
 * but it keeps our threads in sync.
 */
public class InternalLocks {

    /**
     * The lock holder, this holds all the internal locks' state.
     * The string is the path to the lock. If the path to the file is
     * on here then it means a thread is currently writing to the file.
     */
    private static final List<String> writeLocks = Collections.synchronizedList(new ArrayList<>());
    private static final Map<String, AtomicInteger> readLocks = new ConcurrentHashMap<>();

    /**
     * Locks the file for internal writing, this prevents any
     * writers from writing to the file if a thread is currently reading
     * the file.
     *
     * @param path The path to the file.
     * @throws FileLockedException If a thread is currently writing or reading a file, the method will throw
     * this exception to let the thread know and wait.
     */
    public static synchronized void lock(String path) throws FileLockedException {
        if(writeLocks.contains(path) || readLocks.containsKey(path) && readLocks.get(path).get() > 0)
            throw new FileLockedException();

        writeLocks.add(path);
    }

    /**
     * Locks the file for internal reading, this prevents any writers
     * from writing to the file until a thread is finished reading the
     * file.
     *
     * @param path The path to the file.
     * @throws FileLockedException If a thread is currently writing or reading a file, this
     * method will throw this exception.
     */
    public static synchronized void lockRead(String path) throws FileLockedException {
        if(writeLocks.contains(path))
            throw new FileLockedException();

        if(readLocks.containsKey(path))
            readLocks.get(path).addAndGet(1);
        else
            readLocks.put(path, new AtomicInteger(1));
    }

    /**
     * Releases the lock for the file, this will only release
     * the internal thread-lock.
     *
     * @param path The path to the file.
     */
    public static synchronized void release(String path) {
        writeLocks.remove(path);
    }

    /**
     * Releases the lock for the file, this will only release
     * the internal thread-lock for reads.
     *
     * @param path The path to the file.
     */
    public static synchronized void releaseRead(String path) {
        if(readLocks.containsKey(path))
            if(readLocks.get(path).get() > 1)
                readLocks.get(path).decrementAndGet();
            else
                readLocks.remove(path);
    }

    public static class FileLockedException extends Exception {

        public FileLockedException(){
            super("The file is currently locked by another thread, please try again later.");
        }

    }

}
