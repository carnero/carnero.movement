package carnero.movement.common;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.BatteryManager;
import android.text.TextUtils;
import android.view.Display;
import android.view.WindowManager;

import carnero.movement.common.model.Size;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.Wearable;

public class Utils {

    public static float getBatteryLevel() {
        final Intent batteryIntent = App.get().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (batteryIntent == null) {
            return -1f;
        }

        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        return ((float)level / (float)scale) * 100.0f;
    }

    public static String formatDistance(float distance) {
        if (distance > 1600) {
            return String.format(Locale.getDefault(), "%.1f km", distance / 1000f);
        } else {
            return String.format(Locale.getDefault(), "%.1f m", distance);
        }
    }

    public static String getUrlForSize(String url, int size) {
        if (TextUtils.isEmpty(url)) {
            return null;
        }

        return url.replaceAll("/[0-9]\\.jpg", "/" + size + ".jpg");
    }

    public static Asset createAssetFromBitmap(Bitmap bitmap) {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);

        return Asset.createFromBytes(byteStream.toByteArray());
    }

    public static Size getScreenDimensions() {
        Display display = ((WindowManager) App.get().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        return new Size(size.x, size.y);
    }

    public static Bitmap loadBitmapFromAsset(GoogleApiClient apiClient, Asset asset) {
        if (asset == null) {
            return null;
        }

        ConnectionResult result = apiClient.blockingConnect(2000, TimeUnit.MILLISECONDS);
        if (!result.isSuccess()) {
            return null;
        }
        // convert asset into a file descriptor and block until it's ready
        InputStream assetInputStream = Wearable.DataApi.getFdForAsset(apiClient, asset)
            .await()
            .getInputStream();
        apiClient.disconnect();

        if (assetInputStream == null) {
            return null;
        }

        // Decode the stream into a bitmap
        return BitmapFactory.decodeStream(assetInputStream);
    }
}
