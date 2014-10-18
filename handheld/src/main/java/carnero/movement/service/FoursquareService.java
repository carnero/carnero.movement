package carnero.movement.service;

import java.util.List;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;

import carnero.movement.App;
import carnero.movement.common.Preferences;
import carnero.movement.common.remotelog.RemoteLog;
import carnero.movement.db.Helper;
import carnero.movement.model.Checkin;
import carnero.movement.network.foursquare.Api;
import carnero.movement.network.foursquare.Checkins;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class FoursquareService extends Service {

    private Helper mHelper;
    //
    public static final String EXTRA_DOWNLOAD = "download";

    public static void setAlarm(boolean now) {
        final Intent intent = new Intent(App.get(), FoursquareService.class);
        intent.putExtra(FoursquareService.EXTRA_DOWNLOAD, true);
        final PendingIntent pending = PendingIntent.getService(App.get(), 0, intent, 0);

        final AlarmManager manager = (AlarmManager)App.get().getSystemService(Context.ALARM_SERVICE);
        manager.cancel(pending);

        final Preferences preferences = new Preferences();
        if (!preferences.hasFoursquareToken()) {
            return;
        }

        long start = SystemClock.elapsedRealtime();
        if (!now) {
            start += 7 * 60 * 1000; // 7 mins
        }

        manager.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME,
            start,
            AlarmManager.INTERVAL_HOUR * 2, // 2 hrs
            pending
        );
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mHelper = Helper.getInstance();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getBooleanExtra(EXTRA_DOWNLOAD, false)) {
            RemoteLog.i("Requested download of Foursquare checkins");

            downloadCheckins();
            stopSelf();
        }

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void downloadCheckins() {
        final long lastCheckinTime = (mHelper.getLatestCheckinTime() / 1000); // seconds

        Api.get()
            .create(Checkins.class)
            .checkins(lastCheckinTime, Checkins.SORT_NEWEST, 200, new CheckinsCallback());
    }

    // Classes

    private class CheckinsCallback implements Callback<Checkins.CheckinsResponse> {

        @Override
        public void success(Checkins.CheckinsResponse checkinsResponse, Response response) {
            List<Checkin> checkins = checkinsResponse.getCheckins();

            if (checkins != null && !checkins.isEmpty()) {
                for (Checkin checkin : checkins) {
                    mHelper.saveCheckin(checkin);
                }
            }
        }

        @Override
        public void failure(RetrofitError error) {
            RemoteLog.w("Failed to obtain checkins: " + error.getMessage());
        }
    }
}
