package carnero.movement;

import android.app.Application;

import carnero.movement.common.MainThreadBus;

public class App extends Application {

    private static App sInstance;
    private static MainThreadBus sBus;

    @Override
    public void onCreate() {
        super.onCreate();

        sInstance = this;
        sBus = new MainThreadBus();
    }

    public static App get() {
        return sInstance;
    }

    public static MainThreadBus bus() {
        return sBus;
    }
}