package carnero.movement.service;

import android.app.Notification;
import android.location.Location;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.mariux.teleport.lib.TeleportClient;
import com.mariux.teleport.lib.TeleportService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import carnero.movement.App;
import carnero.movement.R;
import carnero.movement.common.Constants;
import carnero.movement.common.Utils;
import carnero.movement.data.Size;

public class ListenerService extends TeleportService {

    private TeleportClient mTeleport;

    @Override
    public void onCreate() {
        super.onCreate();

        mTeleport = new TeleportClient(this);
        mTeleport.connect();

    }

    @Override
    public void onDestroy() {
        mTeleport.disconnect();

        super.onDestroy();
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        final String path = messageEvent.getPath();

        Log.d(Constants.TAG, "Message received: " + path);

        if (path.startsWith(Constants.PATH_RESOLUTION)) {
            Size screenSize = Utils.getScreenDimensions();
            mTeleport.sendMessage(Constants.PATH_RESOLUTION + "?" + screenSize.x + "|" + screenSize.y, null);
        }
    }


    @Override
    public void onDataChanged(DataEventBuffer receivedEvents) {
        Log.d(Constants.TAG, "Data changed");

        final List<DataEvent> events = FreezableUtils.freezeIterable(receivedEvents);

        for (DataEvent event : events) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                DataMapItem item = DataMapItem.fromDataItem(event.getDataItem());
                DataMap map = item.getDataMap();

                if (map.containsKey("status")) {
                    processStatus(map);
                }
            }
        }
    }

    private void processStatus(DataMap data) {
        ArrayList<DataMap> maps = data.getDataMapArrayList("status");
        DataMap map = maps.get(0);

        int steps = map.getInt("steps");
        float distance = map.getFloat("distance");

        Location location = new Location("HANDHELD");
        location.setLatitude(map.getDouble("latitude"));
        location.setLongitude(map.getDouble("longitude"));
        location.setAccuracy(map.getFloat("accuracy"));
        location.setTime(map.getLong("time"));

        App.bus().post(distance);
        App.bus().post(location);

        // Update notification
        final StringBuilder status = new StringBuilder();
        if (distance > 1600) {
            status.append(String.format(Locale.getDefault(), "%.1f", (distance / 1000f)) + " km");
        } else {
            status.append(String.format(Locale.getDefault(), "%.1f", distance) + " m");
        }
        if (steps > 0) {
            status.append(" | ");
            status.append(Integer.toString(steps));
            status.append(" steps");
        }

        final Notification.BigTextStyle style = new Notification.BigTextStyle()
                .bigText(status.toString());

        final Notification.Builder builder = new Notification.Builder(ListenerService.this)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(false)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_launcher)
                .setStyle(style);

        final Notification notification = new Notification.WearableExtender()
                .extend(builder)
                .build();

        NotificationManagerCompat.from(this).notify(1001, notification);
    }
}
