package carnero.movement.common;

import android.graphics.Bitmap;
import android.text.TextUtils;

import com.google.android.gms.wearable.Asset;

import java.io.ByteArrayOutputStream;

public class Utils {

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
