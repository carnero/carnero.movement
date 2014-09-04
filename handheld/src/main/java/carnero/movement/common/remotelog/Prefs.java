package carnero.movement.common.remotelog;

import android.app.Activity;
import android.content.SharedPreferences;

import carnero.movement.App;

/**
 * @author carnero
 */
public class Prefs {

    private static final String PREF_REMOTELOG = "remotelog.pref";
    private static final String PREF_LAST_EMAIL = "last_email";

    public static void saveLastEmail(long time) {
        final SharedPreferences prefs = App.get().getSharedPreferences(PREF_REMOTELOG, Activity.MODE_PRIVATE);
        prefs.edit()
            .putLong(PREF_LAST_EMAIL, time)
            .commit();
    }

    public static long getLastEmail() {
        final SharedPreferences prefs = App.get().getSharedPreferences(PREF_REMOTELOG, Activity.MODE_PRIVATE);
        return prefs.getLong(PREF_LAST_EMAIL, 0);
    }
}
