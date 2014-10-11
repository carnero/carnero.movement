package carnero.movement;

import android.os.StrictMode;

import carnero.movement.common.Application;
import carnero.movement.common.BaseAsyncTask;
import carnero.movement.common.Preferences;
import carnero.movement.common.Utils;
import carnero.movement.common.remotelog.RemoteLog;
import carnero.movement.db.Helper;
import carnero.movement.db.Structure;
import carnero.movement.service.FoursquareService;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;

public class App extends Application {

    private static Tracker sTracker;

    @Override
    public void onCreate() {
        super.onCreate();

        // Set alarm for foursquare
        new FoursquareTask().start();
    }

    public static synchronized Tracker getTracker() {
        if (sTracker == null) {
            final GoogleAnalytics analytics = GoogleAnalytics.getInstance(get());
            sTracker = analytics.newTracker(R.xml.global_tracker);
        }

        return sTracker;
    }

    // Classes

    private class FoursquareTask extends BaseAsyncTask {

        @Override
        public void inBackground() {
            FoursquareService.setAlarm(false);
        }

        @Override
        public void postExecute() {
            // nothing
        }
    }
}
