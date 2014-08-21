package carnero.movement.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageEvent;
import com.mariux.teleport.lib.TeleportClient;
import com.mariux.teleport.lib.TeleportService;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import carnero.movement.App;
import carnero.movement.R;
import carnero.movement.common.Constants;
import carnero.movement.common.Utils;
import carnero.movement.data.ModelDataContainer;
import carnero.movement.data.Size;
import carnero.movement.ui.DistanceActivity;

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
        final ModelDataContainer container = new ModelDataContainer();

        // Summary
        final ArrayList<DataMap> summaryList = data.getDataMapArrayList("summary");
        final DataMap dataMap = summaryList.get(0);

        container.steps = dataMap.getInt("steps");
        container.distance = dataMap.getFloat("distance");

        // Data
        if (data.containsKey("steps")) {
            final ArrayList<DataMap> stepsList = data.getDataMapArrayList("steps");
            for (DataMap map : stepsList) {
                container.stepsList.add(map.getDouble("value"));
            }
        }
        if (data.containsKey("distance")) {
            final ArrayList<DataMap> distanceList = data.getDataMapArrayList("distance");
            for (DataMap map : distanceList) {
                container.distanceList.add(map.getDouble("value"));
            }
        }

        Log.i(Constants.TAG, "Data received: S:" + container.stepsList.size() + " | D:" + container.distanceList.size());

        // Save data
        Bundle extras = new Bundle();
        extras.putParcelable("data", container);

        // Notify activities
        App.bus().post(container);

        // Base notification
        final Intent displayIntent = new Intent(this, DistanceActivity.class);
        displayIntent.putExtras(extras);

        final PendingIntent displayPendingIntent = PendingIntent.getActivity(
                this,
                0,
                displayIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        final Bitmap graphBmp = BitmapFactory.decodeResource(getResources(), R.drawable.background);

        final Notification.BigTextStyle style = new Notification.BigTextStyle()
                .bigText(Utils.formatDistance(container.distance) + "\n" + Integer.toString(container.steps) + " steps");

        final Notification.Builder builder = new Notification.Builder(ListenerService.this)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(false)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(graphBmp)
                .setContentTitle(getString(R.string.app_name))
                .setStyle(style)
                .extend(new Notification.WearableExtender()
                        .setDisplayIntent(displayPendingIntent)
                        .setCustomSizePreset(Notification.WearableExtender.SIZE_MEDIUM));

        final Notification notification = builder.build();
        NotificationManagerCompat.from(this).notify(1001, notification);
    }
}
