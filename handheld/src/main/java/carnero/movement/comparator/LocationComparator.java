package carnero.movement.comparator;

import java.util.Comparator;

import android.location.Location;

public class LocationComparator implements Comparator<Location> {

    @Override
    public int compare(Location lhs, Location rhs) {
        if (lhs.getTime() < rhs.getTime()) {
            return -1;
        } else if (lhs.getTime() > rhs.getTime()) {
            return +1;
        }

        return 0;
    }
}
