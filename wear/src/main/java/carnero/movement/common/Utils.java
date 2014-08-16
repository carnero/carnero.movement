package carnero.movement.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.view.Display;
import android.view.WindowManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.Wearable;

import java.io.InputStream;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import carnero.movement.App;
import carnero.movement.data.Size;

public class Utils {

    public static Size getScreenDimensions() {
        Display display = ((WindowManager) App.get().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);

        return new Size(size.x, size.y);
    }

    public static String formatDistance(float distance) {
        if (distance > 1600) {
            return String.format(Locale.getDefault(), "%.1f km", distance / 1000f);
        } else {
            return String.format(Locale.getDefault(), "%.1f m", distance);
        }
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
