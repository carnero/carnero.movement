package carnero.movement.common.remotelog;

import java.io.*;
import java.util.ArrayList;
import java.util.zip.GZIPOutputStream;

import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;

import carnero.movement.App;
import carnero.movement.common.BaseAsyncTask;
import carnero.movement.common.Constants;
import carnero.movement.common.remotelog.model.LogEntry;
import carnero.movement.common.remotelog.ui.DialogActivity;

/**
 * Logger based on (inmite_fw).RemoteLog
 *
 * @author carnero
 */
public final class RemoteLog implements Thread.UncaughtExceptionHandler {

    private static final boolean sEnabled = true;
    private static String sTagDefault = Constants.TAG;
    private static LogHelper sHelper;
    private static Thread.UncaughtExceptionHandler sDefaultHandler;
    private static long sLastCheck = 0;
    //
    public static final String LOG_FILE = "remote.log.gzip";

    static {
        sHelper = new LogHelper(App.get());
    }

    private static void storeIssue(FailureLevel level, String tag, String message) {
        if (App.get() == null || !App.isDebug() || !sEnabled) {
            return;
        }

        final LogEntry log = new LogEntry(System.currentTimeMillis(), level, tag, message);
        sHelper.insertLog(log);

        if (sLastCheck < (SystemClock.elapsedRealtime() - 5 * 1000)) {
            sLastCheck = SystemClock.elapsedRealtime();

            new SendTask().start();
        }
    }

    /**
     * Check if there is anything to be send
     * If so, get logs, compress and send them
     */
    private static class SendTask extends BaseAsyncTask {

        private boolean mStatus = true;
        private boolean mEnabled = true;

        @Override
        public void inBackground() {
            mEnabled = (Prefs.getLastEmail() < (System.currentTimeMillis() - 15 * 60 * 1000) && sHelper.isThereSomethingBad(FailureLevel.error));
            if (!mEnabled) {
                return; // do nothing
            }

            final ArrayList<LogEntry> logs = sHelper.getLogs();

            try {
                File file = new File(Environment.getExternalStorageDirectory(), LOG_FILE);
                FileOutputStream stream = new FileOutputStream(file);
                GZIPOutputStream gzipStream = new GZIPOutputStream(stream);
                OutputStreamWriter writer = new OutputStreamWriter(gzipStream);
                BufferedWriter bWriter = new BufferedWriter(writer);
                for (LogEntry entry : logs) {
                    bWriter.write(Long.toString(entry.time) + "\t" + entry.level.toString() + "\t\t" + entry.tag + ": " + entry.message + "\n");
                }
                bWriter.close();
            } catch (IOException e) {
                mStatus = false;
            }
        }

        @Override
        public void postExecute() {
            if (!mEnabled) {
                return; // do nothing
            }

            File file = new File(Environment.getExternalStorageDirectory(), LOG_FILE);
            if (App.isDebug() && mStatus && file.exists()) {
                Intent intent = new Intent(App.get(), DialogActivity.class);
                if (Build.VERSION.SDK_INT > 10) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                } else {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                }

                App.get().startActivity(intent);
            }
        }
    }

    /**
     * Set own uncaught exception handler to log crashes
     */
    public static void init() {
        new SendTask().start();

        sDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        if (!(sDefaultHandler instanceof RemoteLog)) {
            Thread.setDefaultUncaughtExceptionHandler(new RemoteLog());
        }
    }

    /**
     * Store crash stacktrace
     *
     * @param t
     * @param e
     */
    public void uncaughtException(Thread t, Throwable e) {
        final Writer writer = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(writer);
        e.printStackTrace(printWriter);

        storeIssue(FailureLevel.crash, sTagDefault, writer.toString());

        sDefaultHandler.uncaughtException(t, e);
    }

    /**
     * Are we in debug mode?
     *
     * @return debug mode
     */
    private static boolean isRemoteLoggingEnabled() {
        return App.isDebug();
    }

    // verbose

    public static int v(String msg) {
        return v(sTagDefault, msg);
    }

    public static int v(String tag, String msg) {
        storeIssue(FailureLevel.verbose, tag, msg);

        int result = 0;
        if (isRemoteLoggingEnabled()) {
            result = Log.v(tag, msg);
        }
        return result;
    }

    public static int v(String tag, String msg, Throwable tr) {
        storeIssue(FailureLevel.verbose, tag, msg);

        int result = 0;
        if (isRemoteLoggingEnabled()) {
            result = Log.v(tag, msg, tr);
        }
        return result;
    }

    // debug

    public static int d(String msg) {
        return d(sTagDefault, msg);
    }

    public static int d(String tag, String msg) {
        storeIssue(FailureLevel.debug, tag, msg);

        int result = 0;
        if (isRemoteLoggingEnabled()) {
            result = Log.d(tag, msg);
        }
        return result;
    }

    public static int d(String tag, String msg, Throwable tr) {
        storeIssue(FailureLevel.debug, tag, msg);

        int result = 0;
        if (isRemoteLoggingEnabled()) {
            result = Log.d(tag, msg, tr);
        }
        return result;
    }

    // information

    public static int i(String msg) {
        return i(sTagDefault, msg);
    }

    public static int i(String tag, String msg) {
        storeIssue(FailureLevel.information, tag, msg);

        int result = 0;
        if (isRemoteLoggingEnabled()) {
            result = Log.i(tag, msg);
        }
        return result;
    }

    public static int i(String tag, String msg, Throwable tr) {
        storeIssue(FailureLevel.information, tag, msg);

        return Log.i(tag, msg, tr);
    }

    // warning

    public static int w(String msg) {
        return w(sTagDefault, msg);
    }

    public static int w(String tag, String msg) {
        storeIssue(FailureLevel.warning, tag, msg);

        return Log.w(tag, msg);
    }

    public static int w(String msg, Throwable tr) {
        return w(sTagDefault, msg, tr);
    }

    public static int w(String tag, String msg, Throwable tr) {
        storeIssue(FailureLevel.warning, tag, msg);

        return Log.w(tag, msg, tr);
    }

    // error

    public static int e(String msg) {
        return e(sTagDefault, msg);
    }

    public static int e(String tag, String msg) {
        storeIssue(FailureLevel.error, tag, msg);

        if (msg != null) {
            return Log.e(tag, msg);
        }
        return 0;
    }

    public static int e(String msg, Throwable tr) {
        return e(sTagDefault, msg, tr);
    }

    public static int e(String tag, String msg, Throwable tr) {
        storeIssue(FailureLevel.error, tag, msg);

        return Log.e(tag, msg, tr);
    }

    // what a terrible failure

    public static int wtf(String msg) {
        return wtf(sTagDefault, msg);
    }

    public static int wtf(String tag, String msg) {
        storeIssue(FailureLevel.terribleFailure, tag, msg);

        return Log.wtf(tag, msg);
    }
}
