package carnero.movement;

import carnero.movement.common.Application;
import carnero.movement.common.Preferences;
import carnero.movement.common.Utils;
import carnero.movement.db.Structure;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Preferences preferences = new Preferences(this);
        if (preferences.getLastBackup() < (System.currentTimeMillis() - (12 * 60 * 60 * 1000))) { // 24 hrs
            boolean status = Utils.backupDatabase(Structure.name);

            if (status) {
                preferences.saveLastBackup(System.currentTimeMillis());
            }
        }

        Utils.restoreDatabase(Structure.name);
    }
}
