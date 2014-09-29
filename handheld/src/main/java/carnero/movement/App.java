package carnero.movement;

import carnero.movement.common.Application;
import carnero.movement.common.Preferences;
import carnero.movement.common.Utils;
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

        // Handle database backup/restore
        Preferences preferences = new Preferences();
        if (preferences.getLastBackup() < (System.currentTimeMillis() - (12 * 60 * 60 * 1000))) { // 24 hrs
            boolean status = Utils.backupDatabase(Structure.name);

            if (status) {
                preferences.saveLastBackup(System.currentTimeMillis());
            }
        }

        Utils.restoreDatabase(Structure.name);

        // Set alarm for foursquare
        FoursquareService.setAlarm(false);
    }

    public static synchronized Tracker getTracker() {
        if (sTracker == null) {
            final GoogleAnalytics analytics = GoogleAnalytics.getInstance(get());
            sTracker = analytics.newTracker(R.xml.global_tracker);
        }

        return sTracker;
    }
}
