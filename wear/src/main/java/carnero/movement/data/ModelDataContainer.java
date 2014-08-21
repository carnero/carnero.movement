package carnero.movement.data;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.Collections;

public class ModelDataContainer implements Parcelable {

    public int steps;
    public float distance;
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
        steps = in.readInt();
        distance = in.readFloat();

        double[] stepsArray = in.createDoubleArray();
        in.readDoubleArray(stepsArray);

        double[] distanceArray = in.createDoubleArray();
        in.readDoubleArray(distanceArray);

        stepsList.clear();
        for (double value : stepsArray) {
            stepsList.add(value);
        }

        distanceList.clear();
        for (double value : stepsArray) {
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

        out.writeInt(steps);
        out.writeFloat(distance);
        out.writeDoubleArray(stepsArray);
        out.writeDoubleArray(distanceArray);
    }

    public int describeContents() {
        return 0;
    }
}
