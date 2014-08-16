package carnero.movement.common;

import android.content.Context;
import android.graphics.Point;
import android.view.Display;
import android.view.WindowManager;

import java.util.Locale;

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
}
