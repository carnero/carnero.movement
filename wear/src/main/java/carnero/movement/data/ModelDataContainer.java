package carnero.movement.data;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

import carnero.movement.common.model.MovementEnum;

public class ModelDataContainer implements Parcelable {

    public int stepsTotal;
    public float distanceTotal;
    public int stepsToday;
    public float distanceToday;
    public double stepsChange;
    public double distanceChange;
    public int movement;
    public final ArrayList<Double> stepsList = new ArrayList<Double>();
    public final ArrayList<Double> distanceList = new ArrayList<Double>();

    public static final Parcelable.Creator<ModelDataContainer> CREATOR = new Parcelable.Creator<ModelDataContainer>() {
        public ModelDataContainer createFromParcel(Parcel in) {
            return new ModelDataContainer(in);
        }

        public ModelDataContainer[] newArray(int size) {
            return new ModelDataContainer[size];
        }
    };

    public ModelDataContainer() {
        // empty
    }

    private ModelDataContainer(Parcel in) {
        final Bundle bundle = in.readBundle();

        stepsTotal = bundle.getInt("stepsTotal");
        distanceTotal = bundle.getFloat("distanceTotal");
        stepsToday = bundle.getInt("stepsToday");
        distanceToday = bundle.getFloat("distanceToday");
        stepsChange = bundle.getDouble("stepsChange");
        distanceChange = bundle.getDouble("distanceChange");
        movement = bundle.getInt("movement");

        double[] stepsArray = bundle.getDoubleArray("stepsList");
        double[] distanceArray = bundle.getDoubleArray("distanceList");

        stepsList.clear();
        for (double value : stepsArray) {
            stepsList.add(value);
        }

        distanceList.clear();
        for (double value : distanceArray) {
            distanceList.add(value);
        }
    }

    public void writeToParcel(Parcel out, int flags) {
        double[] stepsArray = new double[stepsList.size()];
        for (int i = 0; i < stepsList.size(); i ++) {
            stepsArray[i] = stepsList.get(i);
        }

        double[] distanceArray = new double[distanceList.size()];
        for (int i = 0; i < distanceList.size(); i ++) {
            distanceArray[i] = distanceList.get(i);
        }

        final Bundle bundle = new Bundle();
        bundle.putInt("stepsTotal", stepsTotal);
        bundle.putFloat("distanceTotal", distanceTotal);
        bundle.putInt("stepsToday", stepsToday);
        bundle.putFloat("distanceToday", distanceToday);
        bundle.putDouble("stepsChange", stepsChange);
        bundle.putDouble("distanceChange", distanceChange);
        bundle.putInt("movement", movement);
        bundle.putDoubleArray("stepsList", stepsArray);
        bundle.putDoubleArray("distanceList", distanceArray);

        out.writeBundle(bundle);
    }

    public int describeContents() {
        return 0;
    }
}
