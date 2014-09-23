package carnero.movement.receiver;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import carnero.movement.common.Preferences;
import carnero.movement.service.FoursquareService;
import carnero.movement.service.LocationService;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Start location service
        final Intent serviceIntent = new Intent(context, LocationService.class);
        context.startService(serviceIntent);
    }
}
