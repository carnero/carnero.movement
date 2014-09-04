package carnero.movement;

import android.app.Application;

import carnero.movement.common.remotelog.RemoteLog;

public class App extends Application {

    private static App sInstance;
    private static final boolean sRemoteDebug = true;

    @Override
    public void onCreate() {
        super.onCreate();

        sInstance = this;

        RemoteLog.init();
    }

    public static App get() {
        return sInstance;
    }

    public static boolean isDebug() {
        return sRemoteDebug;
    }
}
