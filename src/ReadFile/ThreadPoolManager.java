package ReadFile;


import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * ThreadPoolManager Class wrap ThreadPoolExecutor object with more functionality.
 */
public class ThreadPoolManager {

    //initial variables
    private ThreadPoolExecutor threadPool;
    private InverseSemaphore semaphore;
    private List<Future> futureList;

    /**
     * Constructor.
     *
     * @param numberOfThreads Int. number of Thread.
     */
    public ThreadPoolManager(int numberOfThreads) {

        this.threadPool = (ThreadPoolExecutor) Executors.newFixedThreadPool(numberOfThreads);
        this.semaphore = new InverseSemaphore();
        this.futureList = new ArrayList<>();
    }

    /**
     * Get a Task and add it to the Thread pool.
     *
     * @param command Runnable. new task.
     */
    public void execute(Runnable command) {

        if(command != null) {
            this.semaphore.beforeSubmit();

            this.threadPool.execute(() ->  startTask(command));
        }
    }

    /**
     * inside function that help to use semaphore and
     * count task before and after execute them.
     *
     * @param conmmand Runnable. new task.
     */
    private void startTask(Runnable conmmand) {

        conmmand.run();
        this.semaphore.taskCompleted();
    }

    /**
     * Sleep The calling tread until all threadPool task are done.
     */
    public void sleepUntilAllThreadAreDone() {

        try {
            this.semaphore.awaitCompletion();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * shutdown threadPool.
     */
    public void shutdown() {
        sleepUntilAllThreadAreDone();
        this.threadPool.shutdownNow();
    }

}
