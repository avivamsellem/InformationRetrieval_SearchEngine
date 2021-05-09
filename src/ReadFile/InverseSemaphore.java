package ReadFile;

/**
 * InverseSemaphore Class implement Semaphore, able to find when the counter is zero.\
 * Raise the counter on each task, decrease after task is done.
 */
public class InverseSemaphore {

    private long value = 0;
    private Object lock = new Object();

    /**
     * Task complete will increase the counter by 1.
     */
    public void beforeSubmit() {
        synchronized(lock) {
            value++;
        }
    }

    /**
     * Task complete will decrease the counter by 1.
     */
    public void taskCompleted() {
        synchronized(lock) {
            value--;
            if (value == 0) lock.notifyAll();
        }
    }

    /**
     * Calling thread are waiting until the counter will be zero.
     *
     * @throws InterruptedException
     */
    public void awaitCompletion() throws InterruptedException {
        synchronized(lock) {
            while (value != 0) lock.wait();
        }
    }
}