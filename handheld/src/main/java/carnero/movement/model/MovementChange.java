package carnero.movement.model;

public class MovementChange {

    public int steps;
    public float distance;
    public double stepsChange;
    public double distanceChange;

    public MovementChange(int steps, float distance, double stepsChange, double distanceChange) {
        this.steps = steps;
        this.distance = distance;
        this.stepsChange = stepsChange;
        this.distanceChange = distanceChange;
    }
}
