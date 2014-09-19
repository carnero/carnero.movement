package carnero.movement.common;

import java.io.*;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.os.BatteryManager;
import android.os.Environment;
import android.text.TextUtils;
import android.view.Display;
import android.view.WindowManager;

import carnero.movement.common.model.Size;
import carnero.movement.common.remotelog.RemoteLog;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.Wearable;

@SuppressWarnings("unused")
public class Utils {

    public static float getBatteryLevel() {
        final Intent batteryIntent = Application.get().registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
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
        Display display = ((WindowManager) Application.get().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
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

    public static boolean backupDatabase(String dbName) {
        final File src = new File("/data/data/" + Application.get().getPackageName() + "/databases/" + dbName);
        final File dst = new File(Environment.getExternalStorageDirectory() + "/Movement.backup.db");

        boolean status = copyFile(src, dst);
        if (status) {
            RemoteLog.i("Database '" + dbName + "' is backed up");
        }

        return status;
    }

    public static boolean restoreDatabase(String dbName) {
        final File src = new File(Environment.getExternalStorageDirectory() + "/Movement.db");
        final File dst = new File("/data/data/" + Application.get().getPackageName() + "/databases/" + dbName);

        boolean status = copyFile(src, dst);
        if (status) {
            src.delete();

            RemoteLog.i("Database '" + dbName + "' is restored");
        }

        return status;
    }

    public static boolean copyFile(File src, File dst) {
        try {
            if (src == null || !src.exists() || dst == null) {
                RemoteLog.w("Missing some file");
                return false;
            }

            FileInputStream inputStream = new FileInputStream(src);
            OutputStream outputStream = new FileOutputStream(dst);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.flush();
            outputStream.close();
            inputStream.close();
        } catch (IOException ioe) {
            RemoteLog.e("Failed to copy file");
            return false;
        }

        return true;
    }
}
