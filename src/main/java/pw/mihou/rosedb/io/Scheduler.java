package pw.mihou.rosedb.io;


import pw.mihou.rosedb.RoseDB;

import java.util.concurrent.*;

public class Scheduler {

    private static final int MAXIMUM_POOL_SIZE = Integer.MAX_VALUE;
    private static final int KEEP_ALIVE_TIME = 120;
    private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;

    private static final ExecutorService executorService = new ThreadPoolExecutor(
            RoseDB.cores, MAXIMUM_POOL_SIZE, KEEP_ALIVE_TIME, TIME_UNIT, new SynchronousQueue<>());
    private static final ScheduledExecutorService executor =
            Executors.newScheduledThreadPool(RoseDB.cores);

    public static ExecutorService getExecutorService() {
        return executorService;
    }

    /**
     * Shutdowns the scheduled executor service.
     * Already called when exiting.
     */
    public static void shutdown() {
        executor.shutdown();
    }

    /**
     * Schedules a task at a fixed rate.
     *
     * @param task        the task to schedule.
     * @param delay       the delay before the first run.
     * @param time        the repeat time.
     * @param measurement the time measurement.
     * @return ScheduledFuture<?>
     */
    public static ScheduledFuture<?> schedule(Runnable task, long delay, long time, TimeUnit measurement) {
        return executor.scheduleAtFixedRate(task, delay, time, measurement);
    }

    /**
     * Runs a single task on a delay.
     *
     * @param task        the task to run.
     * @param delay       the delay before the first execution.
     * @param measurement the time measurement.
     * @return ScheduledFuture<?>
     */
    public static ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit measurement) {
        return executor.schedule(task, delay, measurement);
    }

    /**
     * Submits a task to run asynchronous.
     *
     * @param task the task to run.
     * @return CompletableFuture<Void>
     */
    public static CompletableFuture<Void> submitTask(Runnable task) {
        return CompletableFuture.runAsync(task, executor);
    }

    /**
     * Returns the ScheduledExecutorService.
     *
     * @return ScheduledExecutorService.
     */
    public ScheduledExecutorService getScheduler() {
        return executor;
    }

}