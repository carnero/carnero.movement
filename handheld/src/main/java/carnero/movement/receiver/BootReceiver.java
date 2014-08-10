package carnero.movement.receiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import carnero.movement.service.LocationService;

public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        final Intent serviceIntent = new Intent(context, LocationService.class);
        context.startService(serviceIntent);
    }
}
