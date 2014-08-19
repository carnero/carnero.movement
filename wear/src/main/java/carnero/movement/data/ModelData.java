package carnero.movement.data;

import android.support.annotation.NonNull;

public class ModelData implements Comparable<ModelData> {

    public int day;
    public int steps;
    public float distance;

    @Override
    public int compareTo(@NonNull ModelData another) {
        if (this.day > another.day) {
            return +1;
        } else if (this.day < another.day) {
            return -1;
        }

        return 0;
    }
}
