package pw.mihou.rosedb.io;

import org.jetbrains.annotations.NotNull;
import pw.mihou.rosedb.RoseDB;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Scheduler {

    private static final int CORE_POOL_SIZE = RoseDB.cores;
    private static final int MAXIMUM_POOL_SIZE = Integer.MAX_VALUE;
    private static final int KEEP_ALIVE_TIME = 240;
    private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;

    public static final ExecutorService executorService = new ThreadPoolExecutor(
            CORE_POOL_SIZE, MAXIMUM_POOL_SIZE, KEEP_ALIVE_TIME, TIME_UNIT, new SynchronousQueue<>(),
            new ThreadFactory("main", false));

    public static final ScheduledExecutorService scheduledExecutorService =
            Executors.newScheduledThreadPool(CORE_POOL_SIZE, new ThreadFactory("main", false));

    public static ScheduledFuture<?> schedule(Runnable task, long delay, long time, TimeUnit measurement) {
        return scheduledExecutorService.scheduleAtFixedRate(task, delay, time, measurement);
    }

    public static ExecutorService getExecutorService(){
        return executorService;
    }

    public static ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit measurement) {
        return scheduledExecutorService.schedule(task, delay, measurement);
    }

    public static CompletableFuture<Void> submit(Runnable task) {
        return CompletableFuture.runAsync(task, executorService);
    }

    public static void shutdown(){
        executorService.shutdown();
        scheduledExecutorService.shutdown();
    }
}

    class ThreadFactory implements java.util.concurrent.ThreadFactory {

        /**
         * The numbering counter.
         */
        private final AtomicInteger counter = new AtomicInteger();

        /**
         * The name pattern.
         */
        private final String namePattern;

        /**
         * Whether to create daemon threads.
         */
        private final boolean daemon;

        /**
         * Creates a new thread factory.
         *
         * @param namePattern The name pattern, may contain a {@code %d} wildcard where the counter gets filled in.
         * @param daemon Whether to create daemon or non-daemon threads.
         */
        public ThreadFactory(String namePattern, boolean daemon) {
            this.namePattern = namePattern;
            this.daemon = daemon;
        }

        @Override
        public Thread newThread(@NotNull Runnable r) {
            Thread thread = new Thread(r, String.format(namePattern, counter.incrementAndGet()));
            thread.setDaemon(daemon);
            return thread;
        }
}
