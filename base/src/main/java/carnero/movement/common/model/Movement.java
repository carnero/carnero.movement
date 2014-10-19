package carnero.movement.common.model;

public class Movement {

    public MovementEnum type;
    public long timestamp; // ms
    public long startElapsed; // ns
    public long endElapsed; // ns

    public Movement() {
        // empty
    }

    public Movement(MovementEnum type, long timestamp, long startElapsed) {
        this.type = type;
        this.timestamp = timestamp;
        this.startElapsed = startElapsed;
    }
}
