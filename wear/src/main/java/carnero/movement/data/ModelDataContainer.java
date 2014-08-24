package carnero.movement.data;

import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collections;

public class ModelDataContainer implements Parcelable {

    public int steps;
    public float distance;
    public int stepsToday;
    public float distanceToday;
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

        steps = bundle.getInt("steps");
        distance = bundle.getFloat("distance");
        stepsToday = bundle.getInt("stepsToday");
        distanceToday = bundle.getFloat("distanceToday");

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
        bundle.putInt("steps", steps);
        bundle.putFloat("distance", distance);
        bundle.putInt("stepsToday", stepsToday);
        bundle.putFloat("distanceToday", distanceToday);
        bundle.putDoubleArray("stepsList", stepsArray);
        bundle.putDoubleArray("distanceList", distanceArray);

        out.writeBundle(bundle);
    }

    public int describeContents() {
        return 0;
    }
}
