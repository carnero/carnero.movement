package carnero.movement.service;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;
import android.view.Gravity;

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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import carnero.movement.App;
import carnero.movement.R;
import carnero.movement.common.Constants;
import carnero.movement.common.Utils;
import carnero.movement.data.ModelData;
import carnero.movement.data.Size;
import carnero.movement.ui.AbstractBaseActivity;

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
        // Status
        final ArrayList<DataMap> statusList = data.getDataMapArrayList("status");
        final DataMap dataMap = statusList.get(0);

        int steps = dataMap.getInt("steps");
        float distance = dataMap.getFloat("distance");

        Location location = new Location("HANDHELD");
        location.setLatitude(dataMap.getDouble("latitude"));
        location.setLongitude(dataMap.getDouble("longitude"));
        location.setAccuracy(dataMap.getFloat("accuracy"));
        location.setTime(dataMap.getLong("time"));

        Asset graph = dataMap.getAsset("graph");
        Bitmap graphBmp = null;
        if (graph != null) {
            graphBmp = Utils.loadBitmapFromAsset(mTeleport.getGoogleApiClient(), graph);
        }
        if (graphBmp == null) {
            graphBmp = BitmapFactory.decodeResource(getResources(), R.drawable.background);
        }

        // History
        List<ModelData> history = new ArrayList<ModelData>();
        if (data.containsKey("history")) {
            final ArrayList<DataMap> historyList = data.getDataMapArrayList("history");

            Log.d(Constants.TAG, "History: " + historyList.size());

            for (DataMap historyMap : historyList) {
                ModelData entry = new ModelData();
                entry.day = historyMap.getInt("day");
                entry.steps = historyMap.getInt("steps");
                entry.distance = historyMap.getFloat("distance");

                history.add(entry);
            }

            Collections.sort(history);
            Collections.reverse(history);
            history = history.subList(0, 2);
        }

        // Notify activities
        App.bus().post(distance);
        App.bus().post(location);

        // Base notification
        final Notification.BigTextStyle style = new Notification.BigTextStyle()
                .bigText(Utils.formatDistance(distance) + "\n" + Integer.toString(steps) + " steps");

        final Notification.Builder builder = new Notification.Builder(ListenerService.this)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(false)
                .setWhen(System.currentTimeMillis())
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(graphBmp)
                .setContentTitle(getString(R.string.app_name))
                .setStyle(style);

        // Add history to notification
        final Notification.WearableExtender extender = new Notification.WearableExtender();
        for (ModelData entry : history) {
            final Notification.BigTextStyle entryStyle = new Notification.BigTextStyle();
            entryStyle.bigText(Utils.formatDistance(entry.distance) + "\n" + Integer.toString(entry.steps) + " steps");

            Notification.Builder entryBuilder = new Notification.Builder(ListenerService.this)
                    .setLargeIcon(graphBmp)
                    .setStyle(entryStyle);

            if (entry.day == 0) {
                entryBuilder.setContentTitle(getString(R.string.today));
            } else if (entry.day == -1) {
                entryBuilder.setContentTitle(getString(R.string.yesterday));
            } else {
                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.DAY_OF_MONTH, entry.day);

                DateFormat format = SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM);
                String date = format.format(calendar.getTime());

                entryBuilder.setContentTitle(date);
            }

            extender.addPage(entryBuilder.build());
        }

        final Notification notification = extender.extend(builder).build();
        NotificationManagerCompat.from(this).notify(1001, notification);
    }
}
