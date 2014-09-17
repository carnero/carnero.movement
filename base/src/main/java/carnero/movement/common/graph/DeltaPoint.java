package carnero.movement.common.graph;

import android.graphics.Point;

public class DeltaPoint extends Point {

    public int dx;
    public int dy;

    public DeltaPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }
}
