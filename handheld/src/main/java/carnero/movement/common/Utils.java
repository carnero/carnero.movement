package carnero.movement.common;

import java.io.ByteArrayOutputStream;
import java.util.Locale;

import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.BatteryManager;
import android.text.TextUtils;

import carnero.movement.App;
import com.google.android.gms.wearable.Asset;

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
}
