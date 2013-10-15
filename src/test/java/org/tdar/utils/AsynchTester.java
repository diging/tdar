package org.tdar.utils;

/**
 * @author Adam Brin
 *         originally from http://eyalsch.wordpress.com/2010/07/13/multithreaded-tests/
 */
public class AsynchTester {
    private Thread thread;
    private volatile Error error;
    private volatile RuntimeException runtimeExc;

    public AsynchTester(final Runnable runnable) {
        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    runnable.run();
                } catch (Error e) {
                    error = e;
                } catch (RuntimeException e) {
                    runtimeExc = e;
                }
            }
        });
    }

    public void start() {
        thread.start();
    }

    public void test() throws InterruptedException {
        thread.join();
        if (error != null)
            throw error;
        if (runtimeExc != null)
            throw runtimeExc;
    }
}