package carnero.movement.common;

import android.util.Log;

import carnero.movement.common.remotelog.RemoteLog;

public class Application extends android.app.Application {

    private static Application sInstance;
    private static final boolean sRemoteDebug = true;

    @Override
    public void onCreate() {
        super.onCreate();

        sInstance = this;

        RemoteLog.init();
        if (RemoteLog.isEnabled()) {
            Log.e(Constants.TAG, "RemoteLog is ENABLED!");
        }
    }

    public static Application get() {
        return sInstance;
    }

    public static boolean isDebug() {
        return sRemoteDebug;
    }
}
