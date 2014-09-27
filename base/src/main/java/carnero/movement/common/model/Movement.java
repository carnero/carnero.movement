package carnero.movement.common.model;

public class Movement {

    public MovementEnum type;
    public long start; // ns
    public long end; // ns

    public Movement(MovementEnum type) {
        this.type = type;
    }

    public Movement(MovementEnum type, long start) {
        this.type = type;
        this.start = start;
    }
}
