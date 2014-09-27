package carnero.movement.model;

import android.support.annotation.NonNull;

public class OnFootMetrics implements Comparable<OnFootMetrics> {

    public long timestamp; // ns
    public int steps;
    public double cadence; // spm
    public double length; // m
    public double speed; // kph

    @Override
    public int compareTo(@NonNull OnFootMetrics another) {
        if (this.timestamp < another.timestamp) {
            return -1;
        } else if (this.timestamp > another.timestamp) {
            return +1;
        } else {
            return 0;
        }
    }
}
