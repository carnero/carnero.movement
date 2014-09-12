package carnero.movement.db;

public class ModelChange {

    public int steps;
    public float distance;
    public double stepsChange;
    public double distanceChange;

    public ModelChange(int steps, float distance, double stepsChange, double distanceChange) {
        this.steps = steps;
        this.distance = distance;
        this.stepsChange = stepsChange;
        this.distanceChange = distanceChange;
    }
}
