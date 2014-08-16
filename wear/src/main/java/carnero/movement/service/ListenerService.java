package carnero.movement.service;

import android.app.Notification;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.mariux.teleport.lib.TeleportClient;
import com.mariux.teleport.lib.TeleportService;

import java.util.ArrayList;
import java.util.List;

import carnero.movement.App;
import carnero.movement.R;
import carnero.movement.common.Constants;
import carnero.movement.common.Utils;
import carnero.movement.data.Size;

public class ListenerService extends TeleportService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

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

    @Override
    public void onConnected(Bundle bundle) {
        // TODO
    }

    @Override
    public void onConnectionSuspended(int i) {
        // TODO
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // TODO
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

        Asset graph = map.getAsset("graph");
        Bitmap graphBmp = null;
        if (graph != null) {
            graphBmp = Utils.loadBitmapFromAsset(mTeleport.getGoogleApiClient(), graph);
        }

        if (graphBmp == null) {
            graphBmp = BitmapFactory.decodeResource(getResources(), R.drawable.background);
        }

        App.bus().post(distance);
        App.bus().post(location);

        // Update notification
        final Notification.BigTextStyle style = new Notification.BigTextStyle();
        style.setBigContentTitle(getString(R.string.app_name));
        style.bigText(Utils.formatDistance(distance) + "\n" + Integer.toString(steps) + " steps");

        final Notification.Builder builder = new Notification.Builder(ListenerService.this)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(false)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(graphBmp)
                .setStyle(style);

        final Notification notification = new Notification.WearableExtender()
                .extend(builder)
                .build();

        NotificationManagerCompat.from(this).notify(1001, notification);
    }
}
