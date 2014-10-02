package carnero.movement.graph;

import android.content.res.Resources;

import carnero.movement.common.R;
import carnero.movement.common.graph.SplinePath;

public class DistancePath extends SplinePath {

    @Override
    public void init(Resources resources) {
        mFillPath = true;
        mFillColor1Res = R.color.graph_distance;
        mFillColor2Res = R.color.graph_distance;
        mFillGradient = GRADIENT_NONE;
        mStrokeWidthRes = R.dimen.graph_stroke;
        mStrokeColor1Res = R.color.graph_distance;
        mStrokeColor2Res = R.color.graph_distance;
        mStrokeGradient = false;
        mPathWidthRes = R.dimen.graph_path;
        mShowPoints = false;
        mPointColorRes = R.color.none;
        mPointSizeRes = R.dimen.graph_point;
        mPointPaddingRes = R.dimen.graph_point_padding;

        super.init(resources);
    }
}
