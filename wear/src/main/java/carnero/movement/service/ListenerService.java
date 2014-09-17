package carnero.movement.service;

import java.util.ArrayList;
import java.util.List;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import carnero.movement.R;
import carnero.movement.common.Constants;
import carnero.movement.common.Utils;
import carnero.movement.data.ModelDataContainer;
import carnero.movement.common.model.Size;
import carnero.movement.ui.DistanceActivity;
import carnero.movement.ui.StepsActivity;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.*;
import com.mariux.teleport.lib.TeleportClient;
import com.mariux.teleport.lib.TeleportService;

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

                final Uri uri = item.getUri();
                if (uri.getPath().startsWith("/status")) {
                    processStatus(item.getDataMap());
                }
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        // empty
    }

    @Override
    public void onConnectionSuspended(int i) {
        // empty
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // empty
    }

    private void processStatus(DataMap data) {
        final ModelDataContainer container = new ModelDataContainer();

        // Summary
        final ArrayList<DataMap> summaryList = data.getDataMapArrayList("summary");
        final DataMap dataMap = summaryList.get(0);

        long motion = dataMap.getLong("motion"); // debug

        container.stepsTotal = dataMap.getInt("steps_total");
        container.distanceTotal = dataMap.getFloat("distance_total");
        container.stepsToday = dataMap.getInt("steps_today");
        container.distanceToday = dataMap.getFloat("distance_today");
        container.stepsChange = dataMap.getDouble("steps_change");
        container.distanceChange = dataMap.getDouble("distance_change");

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

        // Save data
        Bundle extras = new Bundle();
        extras.putParcelable("data", container);

        // Distance notification
        final Intent distanceIntent = new Intent(this, DistanceActivity.class);
        distanceIntent.putExtras(extras);
        final PendingIntent distancePendingIntent = PendingIntent.getActivity(
            this,
            0,
            distanceIntent,
            PendingIntent.FLAG_UPDATE_CURRENT);

        final Notification.Builder distanceBuilder = new Notification.Builder(ListenerService.this)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(false)
            .setWhen(System.currentTimeMillis())
            .setContentTitle(getString(R.string.app_name));

        new Notification.WearableExtender()
            .setDisplayIntent(distancePendingIntent)
            .setCustomSizePreset(Notification.WearableExtender.SIZE_LARGE)
            .extend(distanceBuilder);

        // Steps notification
        final Intent stepsIntent = new Intent(this, StepsActivity.class);
        stepsIntent.putExtras(extras);
        final PendingIntent stepsPendingIntent = PendingIntent.getActivity(
            this,
            0,
            stepsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT);

        final Notification.Builder stepsBuilder = new Notification.Builder(ListenerService.this)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOngoing(false)
            .setWhen(System.currentTimeMillis())
            .setContentTitle(getString(R.string.app_name));

        new Notification.WearableExtender()
            .setDisplayIntent(stepsPendingIntent)
            .setCustomSizePreset(Notification.WearableExtender.SIZE_LARGE)
            .extend(stepsBuilder);

        // Base notification
        final Bitmap graphBmp = BitmapFactory.decodeResource(getResources(), R.drawable.background);

        double stepsPercent;
        double distancePercent;
        String stepsChange;
        String distanceChange;

        if (container.stepsChange >= 1.0) {
            stepsPercent = (container.stepsChange - 1.0) * 100f;
            stepsChange = "↗";
        } else {
            stepsPercent = (1.0 - container.stepsChange) * 100f;
            stepsChange = "↘";
        }
        if (container.distanceChange >= 1.0) {
            distancePercent = (container.distanceChange - 1.0) * 100f;
            distanceChange = "↗";
        } else {
            distancePercent = (1.0 - container.distanceChange) * 100f;
            distanceChange = "↘";
        }

        String stepsString;
        String distanceString;

        if (stepsPercent < 700) {
            stepsString = String.valueOf((int)stepsPercent) + "%";
        } else {
            stepsString = getString(R.string.stats_lot);
        }
        if (distancePercent < 700) {
            distanceString = String.valueOf((int)distancePercent) + "%";
        } else {
            distanceString = getString(R.string.stats_lot);
        }

        final Notification.BigTextStyle style = new Notification.BigTextStyle();
        style.bigText(
            getString(
                R.string.stats_change,
                Utils.formatDistance(container.distanceToday),
                distanceChange + distanceString,
                container.stepsToday,
                stepsChange + stepsString
            )
        );

        int priority = Notification.PRIORITY_LOW;
        if (motion > System.currentTimeMillis() - (30 * 60 * 1000)) {
            priority = Notification.PRIORITY_HIGH;
        }

        final Notification.Builder builder = new Notification.Builder(ListenerService.this)
            .setPriority(priority)
            .setOngoing(false)
            .setWhen(System.currentTimeMillis())
            .setSmallIcon(R.drawable.ic_notification)
            .setLargeIcon(graphBmp)
                // .setContentTitle(getString(R.string.app_name))
            .setStyle(style);

        final ArrayList<Notification> pages = new ArrayList<Notification>();
        pages.add(distanceBuilder.build());
        pages.add(stepsBuilder.build());

        new Notification.WearableExtender()
            .addPages(pages)
            .extend(builder);

        final Notification notification = builder.build();
        NotificationManagerCompat.from(this).notify(1001, notification);
    }
}
