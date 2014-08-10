package carnero.movement.common;

import android.os.AsyncTask;
import android.os.Build;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Parallel task execution for Honeycomb+, prior Honeycomb it processes tasks one by one.
 * Also it runs on top of executor that is separated from default AsyncTask.THREAD_POOL_EXECUTOR usually
 * used by image loaders etc.
 *
 * @author carnero (carnero@inmite.eu)
 * @author David Vavra (david@inmite.eu)
 */
public abstract class BaseAsyncTask extends AsyncTask<Void, Void, Void> {

    private static int CPU_CORES = Runtime.getRuntime().availableProcessors();
    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable r) {
            return new Thread(r, "BaseAsyncTask #" + mCount.getAndIncrement());
        }
    };

    private static final BlockingQueue<Runnable> sPoolWorkQueue = new LinkedBlockingQueue<Runnable>(256);
    private static ThreadPoolExecutor executor = new ThreadPoolExecutor(
            CPU_CORES + 1,
            (CPU_CORES + 1) * 2,
            1,
            TimeUnit.SECONDS,
            sPoolWorkQueue,
            sThreadFactory
    );

    public void start() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            executor.purge(); // remove cancelled tasks

            executeOnExecutor(executor);
        } else {
            execute();
        }
    }

    @Override
    protected Void doInBackground(Void... params) {
        inBackground();
        return null;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        postExecute();
    }

    public abstract void inBackground();

    public abstract void postExecute();
}